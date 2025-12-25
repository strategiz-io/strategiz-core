"""
Backtest Performance Calculator
"""

import logging
from typing import Dict, List, Any
from datetime import datetime
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
                'total_return': float,      # % return
                'total_pnl': float,         # $ profit/loss
                'win_rate': float,          # % of winning trades
                'total_trades': int,        # Number of completed trades
                'profitable_trades': int,   # Number of winning trades
                'buy_count': int,           # Total BUY signals
                'sell_count': int,          # Total SELL signals
                'avg_win': float,           # Average winning trade $
                'avg_loss': float,          # Average losing trade $
                'profit_factor': float,     # Gross profit / gross loss
                'max_drawdown': float,      # Maximum equity drawdown %
                'sharpe_ratio': float,      # Risk-adjusted return
                'last_tested_at': str,      # ISO timestamp
                'trades': List[Dict],       # Trade history
                'open_position': Dict       # Current open position if any
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
        """Match BUY signals with SELL signals to create completed trades"""

        trades = []

        # Sort by timestamp to ensure chronological order
        buy_signals = sorted(buy_signals, key=lambda x: x['timestamp'])
        sell_signals = sorted(sell_signals, key=lambda x: x['timestamp'])

        buy_idx = 0
        sell_idx = 0

        while buy_idx < len(buy_signals) and sell_idx < len(sell_signals):
            buy = buy_signals[buy_idx]
            sell = sell_signals[sell_idx]

            # SELL must come after BUY
            if sell['timestamp'] <= buy['timestamp']:
                sell_idx += 1
                continue

            # Create trade
            quantity = buy.get('quantity', 1)
            buy_price = buy['price']
            sell_price = sell['price']
            pnl = (sell_price - buy_price) * quantity
            pnl_percent = ((sell_price - buy_price) / buy_price) * 100

            trades.append({
                'buy_timestamp': buy['timestamp'],
                'sell_timestamp': sell['timestamp'],
                'buy_price': buy_price,
                'sell_price': sell_price,
                'quantity': quantity,
                'pnl': pnl,
                'pnl_percent': pnl_percent,
                'win': pnl > 0,
                'buy_reason': buy.get('reason', ''),
                'sell_reason': sell.get('reason', '')
            })

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
            'open_position': None
        }
