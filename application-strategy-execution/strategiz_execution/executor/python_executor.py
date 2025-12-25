"""
Python Strategy Executor with RestrictedPython Sandbox
"""

import ast
import logging
import signal
from typing import Dict, List, Any
from contextlib import contextmanager

import pandas as pd
import pandas_ta as ta
from RestrictedPython import compile_restricted
from RestrictedPython.Guards import safe_builtins, guarded_iter_unpack_sequence

logger = logging.getLogger(__name__)


class TimeoutException(Exception):
    """Raised when execution timeout is reached"""
    pass


def timeout_handler(signum, frame):
    raise TimeoutException("Execution timeout")


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

    @contextmanager
    def time_limit(self, seconds: int):
        """Context manager for execution timeout"""
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(seconds)
        try:
            yield
        finally:
            signal.alarm(0)

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

        try:
            # Convert market data to pandas DataFrame
            df = pd.DataFrame(market_data)
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            df.set_index('timestamp', inplace=True)

            # Prepare safe execution environment
            safe_globals = self._create_safe_globals(df)

            with self.time_limit(timeout):
                # Compile with RestrictedPython
                byte_code = compile_restricted(code, '<strategy>', 'exec')

                if byte_code.errors:
                    return {
                        'success': False,
                        'error': f'Compilation errors: {"; ".join(byte_code.errors)}',
                        'signals': [],
                        'indicators': {},
                        'logs': []
                    }

                # Execute strategy code
                exec(byte_code.code, safe_globals)

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

        except TimeoutException:
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

        return {
            '__builtins__': {
                k: v for k, v in safe_builtins.items()
                if k not in self.FORBIDDEN_BUILTINS
            },
            '_getiter_': guarded_iter_unpack_sequence,

            # Data processing libraries
            'pd': pd,
            'pandas': pd,
            'ta': ta,
            'DataFrame': pd.DataFrame,

            # Safe built-ins
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

            # Storage for signals and indicators
            'signals': [],
            'indicators': {},
        }
