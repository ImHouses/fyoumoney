import { Link } from 'react-router-dom';
import type { BudgetItemResponse } from '../../types/budget';
import {
  formatCurrency,
  parseDecimal,
  getCategoryEmoji,
  getProgressColor,
  getProgressWidth,
} from '../../utils/format';
import './BudgetCategoryRow.css';

interface BudgetCategoryRowProps {
  item: BudgetItemResponse;
  year: number;
  month: number;
  onToggleSnooze?: (itemId: number) => void;
}

export function BudgetCategoryRow({ item, year, month, onToggleSnooze }: BudgetCategoryRowProps) {
  const isIncome = item.categoryType === 'INCOME';
  const remaining = parseDecimal(item.allocation) - parseDecimal(item.spent);
  const isOverBudget = remaining < 0;
  const progressColor = getProgressColor(item.allocation, item.spent);
  const progressWidth = getProgressWidth(item.allocation, item.spent);

  return (
    <Link
      to={`/budgets/${year}/${month}/items/${item.id}/transactions`}
      className={`budget-row${item.snoozed ? ' snoozed' : ''}${isIncome ? ' income' : ''}`}
    >
      <div className="budget-row-name">
        <span className="budget-row-emoji">{getCategoryEmoji(item.categoryName)}</span>
        <span className="budget-row-category-name">{item.categoryName}</span>
      </div>

      {isIncome ? (
        <>
          <span className="budget-row-amount">{formatCurrency(item.spent)}</span>
          <span />
          <span />
        </>
      ) : (
        <>
          <span className="budget-row-amount">{formatCurrency(item.allocation)}</span>
          <span className="budget-row-amount">{formatCurrency(item.spent)}</span>
          <span className={`budget-row-remaining-pill ${isOverBudget ? 'over-budget' : progressColor}`}>
            {isOverBudget ? `-${formatCurrency(String(Math.abs(remaining)))}` : formatCurrency(String(remaining))}
          </span>
        </>
      )}

      {/* Mobile compact view */}
      <div className="budget-row-mobile-right">
        {!isIncome && <div>{formatCurrency(item.allocation)} assigned</div>}
        <div>{formatCurrency(item.spent)} {isIncome ? 'received' : 'spent'}</div>
      </div>

      <button
        className="budget-row-snooze-btn"
        onClick={e => {
          e.preventDefault();
          e.stopPropagation();
          onToggleSnooze?.(item.id);
        }}
        aria-label={item.snoozed ? 'Wake category' : 'Snooze category'}
        aria-pressed={item.snoozed}
      >
        {item.snoozed ? '💤' : '⚡'}
      </button>

      {!isIncome && (
        <div className="budget-row-progress">
          <div
            className={`budget-row-progress-fill ${progressColor}`}
            style={{ width: `${progressWidth}%` }}
          />
        </div>
      )}
    </Link>
  );
}
