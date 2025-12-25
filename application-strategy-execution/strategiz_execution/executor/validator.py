"""
Code Validator using AST Analysis
"""

import ast
import logging
from typing import Dict, List

logger = logging.getLogger(__name__)


class CodeValidator:
    """Validate Python strategy code using AST (Abstract Syntax Tree) analysis"""

    ALLOWED_IMPORTS = {
        'pandas', 'numpy', 'pandas_ta', 'talib', 'math', 'datetime'
    }

    FORBIDDEN_IMPORTS = {
        'os', 'sys', 'subprocess', 'socket', 'requests', 'urllib',
        'pickle', 'shelve', 'multiprocessing', 'threading'
    }

    FORBIDDEN_FUNCTIONS = {
        'eval', 'exec', 'compile', '__import__', 'open', 'input',
        'breakpoint', 'help', 'vars', 'dir', 'globals', 'locals'
    }

    def validate_python(self, code: str) -> Dict[str, any]:
        """
        Validate Python code

        Returns:
            {
                'valid': bool,
                'errors': List[str],
                'warnings': List[str],
                'suggestions': List[str]
            }
        """

        errors = []
        warnings = []
        suggestions = []

        try:
            # Parse code into AST
            tree = ast.parse(code)
        except SyntaxError as e:
            return {
                'valid': False,
                'errors': [f'Syntax error: {e}'],
                'warnings': [],
                'suggestions': []
            }

        # Check for forbidden imports
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    if alias.name in self.FORBIDDEN_IMPORTS:
                        errors.append(f'Forbidden import: {alias.name}')
                    elif alias.name not in self.ALLOWED_IMPORTS:
                        warnings.append(f'Unrecognized import: {alias.name}')

            if isinstance(node, ast.ImportFrom):
                if node.module in self.FORBIDDEN_IMPORTS:
                    errors.append(f'Forbidden import: {node.module}')
                elif node.module not in self.ALLOWED_IMPORTS:
                    warnings.append(f'Unrecognized import: {node.module}')

            # Check for forbidden function calls
            if isinstance(node, ast.Call):
                if isinstance(node.func, ast.Name):
                    if node.func.id in self.FORBIDDEN_FUNCTIONS:
                        errors.append(f'Forbidden function: {node.func.id}()')

        # Check for required strategy function
        has_strategy_func = False
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef):
                if node.name == 'strategy':
                    has_strategy_func = True
                    # Check function signature
                    if len(node.args.args) != 1:
                        errors.append('strategy() must take exactly 1 argument (data)')

        if not has_strategy_func:
            errors.append('Missing required function: def strategy(data)')

        # Check for SYMBOL constant
        has_symbol = 'SYMBOL' in code
        if not has_symbol:
            warnings.append('Consider defining SYMBOL constant')

        # Suggestions
        if 'pandas' not in code and 'pd' not in code:
            suggestions.append('Consider using pandas for data manipulation')

        if 'ta.' not in code and 'pandas_ta' not in code:
            suggestions.append('Consider using pandas-ta for technical indicators')

        return {
            'valid': len(errors) == 0,
            'errors': errors,
            'warnings': warnings,
            'suggestions': suggestions
        }
