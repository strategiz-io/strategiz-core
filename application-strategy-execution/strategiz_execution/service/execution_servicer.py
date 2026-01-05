"""
Strategy Execution gRPC Service Implementation
"""

import logging
import time
from typing import List, Dict, Any

import grpc
from strategiz_execution.generated import strategy_execution_pb2
from strategiz_execution.generated import strategy_execution_pb2_grpc
from strategiz_execution.executor.python_executor import PythonExecutor
from strategiz_execution.executor.validator import CodeValidator
from strategiz_execution.executor.backtest import BacktestCalculator
from strategiz_execution.config import Config
from strategiz_execution.observability import get_observability

logger = logging.getLogger(__name__)


class StrategyExecutionServicer(strategy_execution_pb2_grpc.StrategyExecutionServiceServicer):
    """gRPC service for executing trading strategies"""

    def __init__(self):
        self.config = Config()
        self.python_executor = PythonExecutor(
            timeout_seconds=self.config.max_timeout_seconds
        )
        self.validator = CodeValidator()
        self.backtest_calculator = BacktestCalculator()
        self.observability = get_observability()

    def ExecuteStrategy(self, request, context):
        """Execute a trading strategy"""

        start_time = time.time()
        self.observability.start_execution()

        logger.info(
            f"Executing {request.language} strategy for user={request.user_id}, "
            f"strategy={request.strategy_id}"
        )

        try:
            # Validate request
            self._validate_request(request, context)

            # Convert market data from gRPC to list of dicts
            market_data = self._convert_market_data(request.market_data)

            # Execute based on language
            if request.language.lower() == 'python':
                result = self._execute_python(request, market_data)
            else:
                context.set_code(grpc.StatusCode.UNIMPLEMENTED)
                context.set_details(f'Language {request.language} not supported')

                # Record metrics
                execution_time = int((time.time() - start_time) * 1000)
                self.observability.record_execution(
                    execution_time_ms=execution_time,
                    language=request.language,
                    success=False,
                    error_type="unsupported_language"
                )
                self.observability.end_execution()

                return strategy_execution_pb2.ExecuteStrategyResponse()

            # Set execution time
            execution_time = int((time.time() - start_time) * 1000)
            result.execution_time_ms = execution_time

            # Record successful execution metrics
            self.observability.record_execution(
                execution_time_ms=execution_time,
                language=request.language,
                success=result.success,
                cache_hit=getattr(result, '_cache_hit', False),
                market_data_bars=len(market_data)
            )

            logger.info(f"Strategy execution completed in {execution_time}ms")
            self.observability.end_execution()
            return result

        except ValueError as e:
            execution_time = int((time.time() - start_time) * 1000)
            logger.error(f"Invalid request: {e}")

            # Record error metrics
            self.observability.record_execution(
                execution_time_ms=execution_time,
                language=request.language,
                success=False,
                error_type="validation_error"
            )
            self.observability.end_execution()

            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
            return strategy_execution_pb2.ExecuteStrategyResponse()

        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)
            logger.error(f"Strategy execution failed: {e}", exc_info=True)

            # Record error metrics
            self.observability.record_execution(
                execution_time_ms=execution_time,
                language=request.language,
                success=False,
                error_type=type(e).__name__
            )
            self.observability.end_execution()

            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'Execution failed: {str(e)}')
            return strategy_execution_pb2.ExecuteStrategyResponse()

    def ValidateCode(self, request, context):
        """Validate strategy code without executing"""

        logger.info(f"Validating {request.language} code")

        try:
            if request.language.lower() == 'python':
                validation = self.validator.validate_python(request.code)
            else:
                validation = {
                    'valid': False,
                    'errors': [f'Language {request.language} not supported'],
                    'warnings': [],
                    'suggestions': []
                }

            return strategy_execution_pb2.ValidateCodeResponse(
                valid=validation['valid'],
                errors=validation['errors'],
                warnings=validation['warnings'],
                suggestions=validation['suggestions']
            )

        except Exception as e:
            logger.error(f"Code validation failed: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'Validation failed: {str(e)}')
            return strategy_execution_pb2.ValidateCodeResponse()

    def GetHealth(self, request, context):
        """Health check endpoint"""

        return strategy_execution_pb2.HealthResponse(
            status='SERVING',
            supported_languages=['python'],
            max_timeout_seconds=self.config.max_timeout_seconds,
            max_memory_mb=self.config.max_memory_mb,
            metadata={
                'version': '1.0.0',
                'python_version': '3.11',
                'environment': self.config.environment
            }
        )

    # --- Private Helper Methods ---

    def _validate_request(self, request, context):
        """Validate execution request"""

        if not request.code or not request.code.strip():
            raise ValueError("Code is required")

        if not request.language or not request.language.strip():
            raise ValueError("Language is required")

        if not request.market_data:
            raise ValueError("Market data is required")

        if request.timeout_seconds and request.timeout_seconds > self.config.max_timeout_seconds:
            raise ValueError(f"Timeout exceeds maximum of {self.config.max_timeout_seconds}s")

    def _convert_market_data(self, bars: List) -> List[Dict[str, Any]]:
        """Convert gRPC market data to list of dicts"""

        return [
            {
                'timestamp': bar.timestamp,
                'time': bar.timestamp,
                'open': bar.open,
                'high': bar.high,
                'low': bar.low,
                'close': bar.close,
                'volume': bar.volume
            }
            for bar in bars
        ]

    def _execute_python(self, request, market_data):
        """Execute Python strategy"""

        # Execute code
        execution_result = self.python_executor.execute(
            code=request.code,
            market_data=market_data,
            timeout_seconds=request.timeout_seconds or self.config.max_timeout_seconds
        )

        # Build response
        response = strategy_execution_pb2.ExecuteStrategyResponse()

        if not execution_result['success']:
            response.success = False
            response.error = execution_result.get('error', 'Unknown error')
            response.logs.extend(execution_result.get('logs', []))
            return response

        response.success = True

        # Add signals
        for sig in execution_result.get('signals', []):
            response.signals.append(strategy_execution_pb2.Signal(
                timestamp=sig['timestamp'],
                type=sig['type'],
                price=sig['price'],
                quantity=sig.get('quantity', 1),
                reason=sig.get('reason', '')
            ))

        # Add indicators
        for name, values in execution_result.get('indicators', {}).items():
            indicator = strategy_execution_pb2.Indicator(name=name)
            for i, value in enumerate(values):
                if i < len(market_data):
                    indicator.data.append(strategy_execution_pb2.DataPoint(
                        timestamp=market_data[i]['timestamp'],
                        value=value
                    ))
            response.indicators.append(indicator)

        # Calculate backtest performance
        if execution_result.get('signals'):
            performance = self.backtest_calculator.calculate(
                signals=execution_result['signals'],
                market_data=market_data
            )
            response.performance.CopyFrom(self._convert_performance(performance))

        # Add logs
        response.logs.extend(execution_result.get('logs', []))

        return response

    def _convert_performance(self, perf: Dict) -> strategy_execution_pb2.Performance:
        """Convert performance dict to protobuf"""

        performance = strategy_execution_pb2.Performance(
            total_return=perf['total_return'],
            total_pnl=perf['total_pnl'],
            win_rate=perf['win_rate'],
            total_trades=perf['total_trades'],
            profitable_trades=perf['profitable_trades'],
            buy_count=perf['buy_count'],
            sell_count=perf['sell_count'],
            avg_win=perf['avg_win'],
            avg_loss=perf['avg_loss'],
            profit_factor=perf['profit_factor'],
            max_drawdown=perf['max_drawdown'],
            sharpe_ratio=perf['sharpe_ratio'],
            last_tested_at=perf['last_tested_at'],
            # New fields
            start_date=perf.get('start_date', ''),
            end_date=perf.get('end_date', ''),
            test_period=perf.get('test_period', ''),
            buy_and_hold_return=perf.get('buy_and_hold_return', 0.0),
            buy_and_hold_return_percent=perf.get('buy_and_hold_return_percent', 0.0),
            outperformance=perf.get('outperformance', 0.0)
        )

        # Add trades
        for trade in perf.get('trades', []):
            performance.trades.append(strategy_execution_pb2.Trade(
                buy_timestamp=trade['buy_timestamp'],
                sell_timestamp=trade['sell_timestamp'],
                buy_price=trade['buy_price'],
                sell_price=trade['sell_price'],
                pnl=trade['pnl'],
                pnl_percent=trade['pnl_percent'],
                win=trade['win'],
                buy_reason=trade.get('buy_reason', ''),
                sell_reason=trade.get('sell_reason', '')
            ))

        # Add equity curve
        for point in perf.get('equity_curve', []):
            performance.equity_curve.append(strategy_execution_pb2.EquityPoint(
                timestamp=point.get('timestamp', ''),
                portfolio_value=point.get('portfolioValue', 0.0),
                type=point.get('type', '')
            ))

        return performance
