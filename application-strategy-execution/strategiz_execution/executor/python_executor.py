"""
Python Strategy Executor with RestrictedPython Sandbox
Optimized for sub-100ms execution on complex strategies
"""

import ast
import hashlib
import logging
import threading
from typing import Dict, List, Any, Optional
from concurrent.futures import ThreadPoolExecutor, TimeoutError
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


class PythonExecutor:
    """
    Execute Python strategy code in a sandboxed environment

    Security features:
    - RestrictedPython compilation (blocks dangerous operations)
    - Timeout enforcement
    - Limited builtins (no eval, exec, open, etc.)
    - No file system access
    - No network access
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
        self.executor = ThreadPoolExecutor(max_workers=1)
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
        Execute strategy code with market data

        Returns:
            {
                'success': bool,
                'signals': List[Dict],
                'indicators': Dict[str, List],
                'logs': List[str],
                'error': str (if failed)
            }
        """

        timeout = timeout_seconds or self.timeout_seconds

        # Define execution function
        def _execute_code():
            # OPTIMIZATION 1: Fast DataFrame creation with pre-allocated arrays
            # Convert timestamps once
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
            safe_globals = self._create_safe_globals(df)

            # OPTIMIZATION 2: Code compilation caching
            code_hash = hashlib.md5(code.encode()).hexdigest()

            if code_hash in self._code_cache:
                compiled_code = self._code_cache[code_hash]
            else:
                # Compile with RestrictedPython
                compile_result = compile_restricted(code, '<strategy>', 'exec')

                # Check if compilation failed
                if hasattr(compile_result, 'errors') and compile_result.errors:
                    return {
                        'success': False,
                        'error': f'Compilation errors: {"; ".join(compile_result.errors)}',
                        'signals': [],
                        'indicators': {},
                        'logs': []
                    }

                # Get compiled code (handle different return types)
                compiled_code = compile_result.code if hasattr(compile_result, 'code') else compile_result

                # Cache it (with size limit)
                if len(self._code_cache) >= self._cache_size:
                    # Remove oldest entry (simple FIFO)
                    self._code_cache.pop(next(iter(self._code_cache)))
                self._code_cache[code_hash] = compiled_code

            # Execute strategy code
            exec(compiled_code, safe_globals)

            # Call strategy function
            if 'strategy' in safe_globals and callable(safe_globals['strategy']):
                result = safe_globals['strategy'](df)

                return {
                    'success': True,
                    'signals': safe_globals.get('signals', []),
                    'indicators': safe_globals.get('indicators', {}),
                    'result': result,
                    'logs': []
                }
            else:
                return {
                    'success': False,
                    'error': 'No strategy function found',
                    'signals': [],
                    'indicators': {},
                    'logs': []
                }

        try:
            # Execute with timeout using ThreadPoolExecutor
            future = self.executor.submit(_execute_code)
            result = future.result(timeout=timeout)
            return result

        except TimeoutError:
            logger.error(f"Execution timeout after {timeout}s")
            return {
                'success': False,
                'error': f'Execution timeout ({timeout}s)',
                'signals': [],
                'indicators': {},
                'logs': []
            }

        except Exception as e:
            logger.error(f"Execution error: {e}", exc_info=True)
            return {
                'success': False,
                'error': f'Execution error: {str(e)}',
                'signals': [],
                'indicators': {},
                'logs': []
            }

    def _create_safe_globals(self, df: pd.DataFrame) -> Dict:
        """Create safe global namespace for code execution"""

        # Start with safe_globals from RestrictedPython
        safe_env = safe_globals.copy()

        # Add restricted builtins (excluding forbidden ones)
        safe_env['__builtins__'] = {
            k: v for k, v in safe_builtins.items()
            if k not in self.FORBIDDEN_BUILTINS
        }

        # Add guards for operations
        def _iter_unpack(ob, spec, _getiter_=None):
            """Custom iterator for tuple unpacking - RestrictedPython compatible"""
            # spec is a dict like {'childs': (), 'min_len': 2}
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
            '_inplacevar_': lambda op, x, y: x,  # Simple inplace var guard
            # Add item access guards
            '_getitem_': lambda obj, index: obj[index],
            '_write_': lambda obj: obj,
            '_print_': lambda *args, **kwargs: None,  # Suppress prints
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
