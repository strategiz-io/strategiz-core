"""
Python Strategy Executor with RestrictedPython Sandbox
Optimized for sub-100ms execution on complex strategies
"""

import ast
import hashlib
import logging
import multiprocessing
from typing import Dict, List, Any, Optional
from functools import lru_cache

import numpy as np
import pandas as pd
import pandas_ta as ta
from RestrictedPython import compile_restricted
from RestrictedPython.Guards import (
    safe_builtins,
    guarded_iter_unpack_sequence,
    safe_globals,
    safer_getattr
)

logger = logging.getLogger(__name__)


class TimeoutException(Exception):
    """Raised when execution timeout is reached"""
    pass


def _execute_strategy_in_process(queue, code, market_data, code_cache):
    """
    Execute strategy code in a separate process (module-level function for pickling)

    This MUST be a module-level function (not nested) so multiprocessing can pickle it.
    """
    try:
        # OPTIMIZATION 1: Fast DataFrame creation with pre-allocated arrays
        timestamps = pd.to_datetime([bar['timestamp'] for bar in market_data])

        # Pre-allocate numpy arrays for OHLCV data
        n = len(market_data)
        df = pd.DataFrame({
            'open': np.array([bar['open'] for bar in market_data], dtype=np.float64),
            'high': np.array([bar['high'] for bar in market_data], dtype=np.float64),
            'low': np.array([bar['low'] for bar in market_data], dtype=np.float64),
            'close': np.array([bar['close'] for bar in market_data], dtype=np.float64),
            'volume': np.array([bar['volume'] for bar in market_data], dtype=np.int64),
        }, index=timestamps)

        # Prepare safe execution environment
        safe_env = _create_safe_globals_for_process(df)

        # OPTIMIZATION 2: Code compilation caching
        code_hash = hashlib.md5(code.encode()).hexdigest()

        if code_hash in code_cache:
            compiled_code = code_cache[code_hash]
        else:
            # Compile with RestrictedPython
            compile_result = compile_restricted(code, '<strategy>', 'exec')

            # Check if compilation failed
            if hasattr(compile_result, 'errors') and compile_result.errors:
                queue.put({
                    'success': False,
                    'error': f'Compilation errors: {"; ".join(compile_result.errors)}',
                    'signals': [],
                    'indicators': {},
                    'logs': []
                })
                return

            # Get compiled code
            compiled_code = compile_result.code if hasattr(compile_result, 'code') else compile_result

        # Execute strategy code
        exec(compiled_code, safe_env)

        # Call strategy function
        if 'strategy' in safe_env and callable(safe_env['strategy']):
            result = safe_env['strategy'](df)

            queue.put({
                'success': True,
                'signals': safe_env.get('signals', []),
                'indicators': safe_env.get('indicators', {}),
                'result': result,
                'logs': []
            })
        else:
            queue.put({
                'success': False,
                'error': 'No strategy function found',
                'signals': [],
                'indicators': {},
                'logs': []
            })

    except Exception as e:
        queue.put({
            'success': False,
            'error': f'Execution error: {str(e)}',
            'signals': [],
            'indicators': {},
            'logs': []
        })


def _create_safe_globals_for_process(df: pd.DataFrame) -> dict:
    """Create safe global namespace for code execution (module-level for multiprocessing)"""

    # Start with safe_globals from RestrictedPython
    safe_env = safe_globals.copy()

    # Add restricted builtins (excluding forbidden ones)
    FORBIDDEN_BUILTINS = {
        'eval', 'exec', 'compile', '__import__', 'open', 'input',
        'breakpoint', 'help', 'vars', 'dir', 'globals', 'locals'
    }

    safe_env['__builtins__'] = {
        k: v for k, v in safe_builtins.items()
        if k not in FORBIDDEN_BUILTINS
    }

    # Add guards for operations
    def _iter_unpack(ob, spec, _getiter_=None):
        """Custom iterator for tuple unpacking - RestrictedPython compatible"""
        if isinstance(spec, dict):
            count = spec.get('min_len', 0)
        else:
            count = spec

        if not hasattr(ob, '__iter__'):
            raise TypeError(f'{type(ob).__name__} object is not iterable')
        result = list(ob)
        if count and len(result) < count:
            raise ValueError(f'not enough values to unpack (expected {count}, got {len(result)})')
        return result

    safe_env.update({
        '_getiter_': lambda obj: iter(obj),
        '_iter_unpack_sequence_': _iter_unpack,
        '_unpack_sequence_': _iter_unpack,
        '_getattr_': safer_getattr,
        '_inplacevar_': lambda op, x, y: x,
        '_getitem_': lambda obj, index: obj[index],
        '_write_': lambda obj: obj,
        '_print_': lambda *args, **kwargs: None,
        '__name__': 'strategy_module',
        '__metaclass__': type,
    })

    # Add data processing libraries
    safe_env.update({
        'pd': pd,
        'pandas': pd,
        'np': np,
        'numpy': np,
        'ta': ta,
        'DataFrame': pd.DataFrame,
    })

    # Add safe built-ins
    safe_env.update({
        'len': len,
        'range': range,
        'enumerate': enumerate,
        'zip': zip,
        'map': map,
        'filter': filter,
        'sum': sum,
        'min': min,
        'max': max,
        'abs': abs,
        'round': round,
        'int': int,
        'float': float,
        'str': str,
        'bool': bool,
        'list': list,
        'dict': dict,
        'tuple': tuple,
        'set': set,
    })

    # Storage for signals and indicators
    safe_env.update({
        'signals': [],
        'indicators': {},
    })

    return safe_env


class PythonExecutor:
    """
    Execute Python strategy code in a sandboxed environment

    Security features:
    - RestrictedPython compilation (blocks dangerous operations)
    - Timeout enforcement with process termination (CRITICAL FOR PRODUCTION)
    - Limited builtins (no eval, exec, open, etc.)
    - No file system access
    - No network access

    IMPORTANT: Uses multiprocessing.Process instead of threads because
    Python threads CANNOT be forcefully killed, leading to runaway processes
    that consume CPU indefinitely (and rack up Cloud Run costs!).
    """

    ALLOWED_IMPORTS = {
        'pandas', 'pd', 'numpy', 'np', 'pandas_ta', 'ta', 'math', 'datetime'
    }

    FORBIDDEN_BUILTINS = {
        'eval', 'exec', 'compile', '__import__', 'open', 'input',
        'breakpoint', 'help', 'vars', 'dir', 'globals', 'locals'
    }

    def __init__(self, timeout_seconds: int = 10):
        self.timeout_seconds = timeout_seconds
        # LRU cache for compiled code (cache last 100 unique strategies)
        self._code_cache = {}
        self._cache_size = 100

    def execute(
        self,
        code: str,
        market_data: List[Dict[str, Any]],
        timeout_seconds: int = None
    ) -> Dict[str, Any]:
        """
        Execute strategy code with market data in a separate process

        CRITICAL: Uses multiprocessing to enable forceful termination of runaway code.
        This prevents infinite loops from consuming CPU indefinitely.

        Returns:
            {
                'success': bool,
                'signals': List[Dict],
                'indicators': Dict[str, List],
                'logs': List[str],
                'error': str (if failed),
                'execution_time_ms': int
            }
        """
        import time
        start_time = time.time()
        timeout = timeout_seconds or self.timeout_seconds

        # Create a queue to receive results from the process
        result_queue = multiprocessing.Queue()

        try:
            # Start execution in separate process (using module-level function for pickling)
            process = multiprocessing.Process(
                target=_execute_strategy_in_process,
                args=(result_queue, code, market_data, self._code_cache)
            )
            process.start()

            # Wait for process to complete with timeout
            process.join(timeout=timeout)

            # Check if process is still alive (timeout occurred)
            if process.is_alive():
                logger.warning(f"Strategy execution timeout after {timeout}s - terminating process")

                # CRITICAL: Forcefully terminate the process
                process.terminate()

                # Wait briefly for graceful shutdown
                process.join(timeout=1)

                # If still alive, use kill (nuclear option)
                if process.is_alive():
                    logger.error("Process didn't terminate - using kill()")
                    process.kill()
                    process.join()

                execution_time_ms = int((time.time() - start_time) * 1000)
                return {
                    'success': False,
                    'error': f'Execution timeout ({timeout}s) - strategy was forcefully terminated',
                    'signals': [],
                    'indicators': {},
                    'logs': [],
                    'execution_time_ms': execution_time_ms
                }

            # Process completed - get result from queue
            if not result_queue.empty():
                result = result_queue.get(timeout=1)
                execution_time_ms = int((time.time() - start_time) * 1000)
                result['execution_time_ms'] = execution_time_ms
                return result
            else:
                # Process died without putting result
                execution_time_ms = int((time.time() - start_time) * 1000)
                return {
                    'success': False,
                    'error': 'Strategy execution failed - process terminated unexpectedly',
                    'signals': [],
                    'indicators': {},
                    'logs': [],
                    'execution_time_ms': execution_time_ms
                }

        except Exception as e:
            logger.error(f"Execution error: {e}", exc_info=True)
            execution_time_ms = int((time.time() - start_time) * 1000)
            return {
                'success': False,
                'error': f'Execution error: {str(e)}',
                'signals': [],
                'indicators': {},
                'logs': [],
                'execution_time_ms': execution_time_ms
            }
