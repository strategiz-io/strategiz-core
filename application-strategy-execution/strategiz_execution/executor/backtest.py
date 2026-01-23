"""
Backtest Performance Calculator
"""

import logging
from typing import Dict, List, Any
from datetime import datetime
from dateutil import parser as date_parser
from dateutil.relativedelta import relativedelta
import math

logger = logging.getLogger(__name__)


class BacktestCalculator:
    """Calculate trading strategy performance from signals and market data"""

    def __init__(self, initial_capital: float = 10000.0):
        self.initial_capital = initial_capital

    def calculate(
        self,
        signals: List[Dict[str, Any]],
        market_data: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Calculate performance metrics from trading signals

        Args:
            signals: List of trading signals with timestamp, type, price, quantity, reason
            market_data: List of market bars with timestamp, open, high, low, close, volume

        Returns:
            {
                'total_return': float,           # % return
                'total_pnl': float,              # $ profit/loss
                'win_rate': float,               # % of winning trades
                'total_trades': int,             # Number of completed trades
                'profitable_trades': int,        # Number of winning trades
                'buy_count': int,                # Total BUY signals
                'sell_count': int,               # Total SELL signals
                'avg_win': float,                # Average winning trade $
                'avg_loss': float,               # Average losing trade $
                'profit_factor': float,          # Gross profit / gross loss
                'max_drawdown': float,           # Maximum equity drawdown %
                'sharpe_ratio': float,           # Risk-adjusted return
                'last_tested_at': str,           # ISO timestamp
                'trades': List[Dict],            # Trade history
                'open_position': Dict,           # Current open position if any
                'equity_curve': List[Dict],      # Portfolio value over time
                'start_date': str,               # Backtest start date
                'end_date': str,                 # Backtest end date
                'test_period': str,              # Human-readable test duration
                'buy_and_hold_return': float,    # $ P&L if just held
                'buy_and_hold_return_percent': float,  # % return if just held
                'outperformance': float          # Strategy return - buy & hold return (%)
            }
        """

        if not signals:
            return self._empty_performance()

        # Separate BUY and SELL signals
        buy_signals = [s for s in signals if s.get('type', '').upper() == 'BUY']
        sell_signals = [s for s in signals if s.get('type', '').upper() == 'SELL']

        # Match BUY and SELL signals to create trades
        trades = self._match_trades(buy_signals, sell_signals)

        # Calculate performance metrics
        performance = self._calculate_metrics(
            trades=trades,
            buy_count=len(buy_signals),
            sell_count=len(sell_signals)
        )

        # Build equity curve
        equity_curve = self._build_equity_curve(trades, market_data)
        performance['equity_curve'] = equity_curve

        # Calculate test period info
        if market_data:
            start_date = market_data[0].get('timestamp', '')
            end_date = market_data[-1].get('timestamp', '')
            performance['start_date'] = start_date
            performance['end_date'] = end_date
            performance['test_period'] = self._format_test_period(start_date, end_date)

            # Calculate buy & hold comparison
            first_close = float(market_data[0].get('close', 0))
            last_close = float(market_data[-1].get('close', 0))

            if first_close > 0:
                # Calculate buy & hold returns (assuming we buy with initial capital)
                shares_bought = self.initial_capital / first_close
                buy_hold_final_value = shares_bought * last_close
                buy_hold_pnl = buy_hold_final_value - self.initial_capital
                buy_hold_return_percent = ((last_close - first_close) / first_close) * 100

                performance['buy_and_hold_return'] = buy_hold_pnl
                performance['buy_and_hold_return_percent'] = buy_hold_return_percent
                performance['outperformance'] = performance['total_return'] - buy_hold_return_percent
            else:
                performance['buy_and_hold_return'] = 0.0
                performance['buy_and_hold_return_percent'] = 0.0
                performance['outperformance'] = 0.0
        else:
            performance['start_date'] = ''
            performance['end_date'] = ''
            performance['test_period'] = ''
            performance['buy_and_hold_return'] = 0.0
            performance['buy_and_hold_return_percent'] = 0.0
            performance['outperformance'] = 0.0

        # Check for open position
        if len(buy_signals) > len(sell_signals):
            last_buy = buy_signals[-1]
            current_price = market_data[-1]['close'] if market_data else last_buy['price']
            performance['open_position'] = {
                'entry_timestamp': last_buy['timestamp'],
                'entry_price': last_buy['price'],
                'current_price': current_price,
                'unrealized_pnl': (current_price - last_buy['price']) * last_buy.get('quantity', 1),
                'unrealized_pnl_percent': ((current_price - last_buy['price']) / last_buy['price']) * 100,
                'reason': last_buy.get('reason', '')
            }
        else:
            performance['open_position'] = None

        performance['last_tested_at'] = datetime.utcnow().isoformat() + 'Z'

        return performance

    def _match_trades(
        self,
        buy_signals: List[Dict],
        sell_signals: List[Dict]
    ) -> List[Dict]:
        """
        Match BUY signals with SELL signals to create completed trades.

        IMPORTANT: Calculates proper position sizing based on available capital.
        This allows compounding - profits from one trade increase capital for the next.
        """

        trades = []

        # Sort by timestamp to ensure chronological order
        buy_signals = sorted(buy_signals, key=lambda x: x['timestamp'])
        sell_signals = sorted(sell_signals, key=lambda x: x['timestamp'])

        buy_idx = 0
        sell_idx = 0

        # Track available capital for proper position sizing
        available_capital = self.initial_capital

        while buy_idx < len(buy_signals) and sell_idx < len(sell_signals):
            buy = buy_signals[buy_idx]
            sell = sell_signals[sell_idx]

            # SELL must come after BUY
            if sell['timestamp'] <= buy['timestamp']:
                sell_idx += 1
                continue

            buy_price = buy['price']
            sell_price = sell['price']

            # Calculate proper position size based on available capital
            # Use 100% of available capital for maximum returns
            if buy_price > 0:
                quantity = int(available_capital / buy_price)
            else:
                quantity = buy.get('quantity', 1)

            # Ensure at least 1 share
            quantity = max(quantity, 1)

            # Calculate P&L
            invested_amount = quantity * buy_price
            exit_amount = quantity * sell_price
            pnl = exit_amount - invested_amount
            pnl_percent = ((sell_price - buy_price) / buy_price) * 100

            trades.append({
                'buy_timestamp': buy['timestamp'],
                'sell_timestamp': sell['timestamp'],
                'buy_price': buy_price,
                'sell_price': sell_price,
                'quantity': quantity,
                'invested_amount': invested_amount,
                'exit_amount': exit_amount,
                'pnl': pnl,
                'pnl_percent': pnl_percent,
                'win': pnl > 0,
                'buy_reason': buy.get('reason', ''),
                'sell_reason': sell.get('reason', ''),
                'capital_before': available_capital,
                'capital_after': available_capital + pnl
            })

            # Update available capital for next trade (compounding!)
            available_capital += pnl

            buy_idx += 1
            sell_idx += 1

        return trades

    def _calculate_metrics(
        self,
        trades: List[Dict],
        buy_count: int,
        sell_count: int
    ) -> Dict[str, Any]:
        """Calculate performance metrics from completed trades"""

        if not trades:
            return {
                'total_return': 0.0,
                'total_pnl': 0.0,
                'win_rate': 0.0,
                'total_trades': 0,
                'profitable_trades': 0,
                'buy_count': buy_count,
                'sell_count': sell_count,
                'avg_win': 0.0,
                'avg_loss': 0.0,
                'profit_factor': 0.0,
                'max_drawdown': 0.0,
                'sharpe_ratio': 0.0,
                'trades': []
            }

        # Basic metrics
        total_pnl = sum(t['pnl'] for t in trades)
        total_return = (total_pnl / self.initial_capital) * 100
        profitable_trades = sum(1 for t in trades if t['win'])
        win_rate = (profitable_trades / len(trades)) * 100 if trades else 0.0

        # Win/Loss averages
        winning_trades = [t['pnl'] for t in trades if t['win']]
        losing_trades = [t['pnl'] for t in trades if not t['win']]

        avg_win = sum(winning_trades) / len(winning_trades) if winning_trades else 0.0
        avg_loss = sum(losing_trades) / len(losing_trades) if losing_trades else 0.0

        # Profit factor
        gross_profit = sum(winning_trades) if winning_trades else 0.0
        gross_loss = abs(sum(losing_trades)) if losing_trades else 0.0
        profit_factor = gross_profit / gross_loss if gross_loss > 0 else (gross_profit if gross_profit > 0 else 0.0)

        # Max drawdown
        max_drawdown = self._calculate_max_drawdown(trades)

        # Sharpe ratio
        sharpe_ratio = self._calculate_sharpe_ratio(trades)

        return {
            'total_return': total_return,
            'total_pnl': total_pnl,
            'win_rate': win_rate,
            'total_trades': len(trades),
            'profitable_trades': profitable_trades,
            'buy_count': buy_count,
            'sell_count': sell_count,
            'avg_win': avg_win,
            'avg_loss': avg_loss,
            'profit_factor': profit_factor,
            'max_drawdown': max_drawdown,
            'sharpe_ratio': sharpe_ratio,
            'trades': trades
        }

    def _calculate_max_drawdown(self, trades: List[Dict]) -> float:
        """Calculate maximum drawdown percentage"""

        if not trades:
            return 0.0

        # Build equity curve
        equity = self.initial_capital
        peak = equity
        max_dd = 0.0

        for trade in trades:
            equity += trade['pnl']

            # Update peak
            if equity > peak:
                peak = equity

            # Calculate drawdown from peak
            if peak > 0:
                drawdown = ((peak - equity) / peak) * 100
                max_dd = max(max_dd, drawdown)

        return max_dd

    def _calculate_sharpe_ratio(self, trades: List[Dict]) -> float:
        """
        Calculate Sharpe ratio (risk-adjusted return)

        Sharpe = (Average Return - Risk-Free Rate) / Std Dev of Returns
        Assuming risk-free rate = 0 for simplicity
        """

        if not trades or len(trades) < 2:
            return 0.0

        returns = [t['pnl_percent'] for t in trades]

        # Average return
        avg_return = sum(returns) / len(returns)

        # Standard deviation
        variance = sum((r - avg_return) ** 2 for r in returns) / len(returns)
        std_dev = math.sqrt(variance)

        # Sharpe ratio (annualized approximation)
        if std_dev == 0:
            return 0.0

        sharpe = avg_return / std_dev

        # Annualize (assuming ~252 trading days)
        # This is a rough approximation
        sharpe_annualized = sharpe * math.sqrt(252 / len(trades))

        return sharpe_annualized

    def _build_equity_curve(
        self,
        trades: List[Dict],
        market_data: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Build equity curve showing portfolio value over time.

        Returns list of points:
        [
            {'timestamp': str, 'portfolioValue': float, 'type': 'initial'|'buy'|'sell'}
        ]
        """
        equity_curve = []

        if not market_data:
            return equity_curve

        # Start with initial capital at first market data point
        start_timestamp = market_data[0].get('timestamp', '')
        equity_curve.append({
            'timestamp': start_timestamp,
            'portfolioValue': self.initial_capital,
            'type': 'initial'
        })

        if not trades:
            # No trades - just show initial and final (unchanged)
            end_timestamp = market_data[-1].get('timestamp', '')
            if end_timestamp != start_timestamp:
                equity_curve.append({
                    'timestamp': end_timestamp,
                    'portfolioValue': self.initial_capital,
                    'type': 'final'
                })
            return equity_curve

        # Build curve with each trade
        equity = self.initial_capital

        for trade in trades:
            # Record buy point (portfolio value before trade)
            equity_curve.append({
                'timestamp': trade['buy_timestamp'],
                'portfolioValue': equity,
                'type': 'buy'
            })

            # After sell, add P&L
            equity += trade['pnl']
            equity_curve.append({
                'timestamp': trade['sell_timestamp'],
                'portfolioValue': equity,
                'type': 'sell'
            })

        # Add final point at end of market data if different from last trade
        end_timestamp = market_data[-1].get('timestamp', '')
        if equity_curve and equity_curve[-1]['timestamp'] != end_timestamp:
            equity_curve.append({
                'timestamp': end_timestamp,
                'portfolioValue': equity,
                'type': 'final'
            })

        return equity_curve

    def _format_test_period(self, start_date: str, end_date: str) -> str:
        """
        Format the test period as human-readable duration.

        Examples:
        - "2 years, 3 months"
        - "6 months, 15 days"
        - "45 days"
        """
        try:
            start = date_parser.parse(start_date)
            end = date_parser.parse(end_date)

            delta = relativedelta(end, start)

            parts = []

            if delta.years > 0:
                parts.append(f"{delta.years} year{'s' if delta.years > 1 else ''}")

            if delta.months > 0:
                parts.append(f"{delta.months} month{'s' if delta.months > 1 else ''}")

            if delta.days > 0 and delta.years == 0:  # Only show days if less than a year
                parts.append(f"{delta.days} day{'s' if delta.days > 1 else ''}")

            if not parts:
                # Less than a day
                return "< 1 day"

            return ", ".join(parts)

        except Exception as e:
            logger.warning(f"Failed to parse test period dates: {e}")
            return ""

    def _empty_performance(self) -> Dict[str, Any]:
        """Return empty performance when no signals"""

        return {
            'total_return': 0.0,
            'total_pnl': 0.0,
            'win_rate': 0.0,
            'total_trades': 0,
            'profitable_trades': 0,
            'buy_count': 0,
            'sell_count': 0,
            'avg_win': 0.0,
            'avg_loss': 0.0,
            'profit_factor': 0.0,
            'max_drawdown': 0.0,
            'sharpe_ratio': 0.0,
            'last_tested_at': datetime.utcnow().isoformat() + 'Z',
            'trades': [],
            'open_position': None,
            'equity_curve': [],
            'start_date': '',
            'end_date': '',
            'test_period': '',
            'buy_and_hold_return': 0.0,
            'buy_and_hold_return_percent': 0.0,
            'outperformance': 0.0
        }
