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
}

export function BudgetCategoryRow({ item, year, month }: BudgetCategoryRowProps) {
  const remaining = parseDecimal(item.allocation) - parseDecimal(item.spent);
  const isOverBudget = remaining < 0;
  const progressColor = getProgressColor(item.allocation, item.spent);
  const progressWidth = getProgressWidth(item.allocation, item.spent);

  return (
    <Link
      to={`/budgets/${year}/${month}/items/${item.id}/transactions`}
      className={`budget-row${item.snoozed ? ' snoozed' : ''}`}
    >
      <div className="budget-row-name">
        <span className="budget-row-emoji">{getCategoryEmoji(item.categoryName)}</span>
        <span className="budget-row-category-name">{item.categoryName}</span>
      </div>

      <span className="budget-row-amount">{formatCurrency(item.allocation)}</span>
      <span className="budget-row-amount">{formatCurrency(item.spent)}</span>

      <span className={`budget-row-remaining-pill ${isOverBudget ? 'over-budget' : progressColor}`}>
        {isOverBudget ? `-${formatCurrency(String(Math.abs(remaining)))}` : formatCurrency(String(remaining))}
      </span>

      {/* Mobile compact view */}
      <div className="budget-row-mobile-right">
        <div>{formatCurrency(item.allocation)} assigned</div>
        <div>{formatCurrency(item.spent)} spent</div>
      </div>

      <div className="budget-row-progress">
        <div
          className={`budget-row-progress-fill ${progressColor}`}
          style={{ width: `${progressWidth}%` }}
        />
      </div>
    </Link>
  );
}
