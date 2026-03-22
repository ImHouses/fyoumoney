import type { BudgetResponse } from '../../types/budget';
import { formatCurrency, parseDecimal } from '../../utils/format';
import './SummaryPanel.css';

interface SummaryPanelProps {
  budget: BudgetResponse;
}

export function SummaryPanel({ budget }: SummaryPanelProps) {
  const totalAssigned = budget.items
    .filter(item => !item.snoozed && item.categoryType === 'EXPENSE')
    .reduce((sum, item) => sum + parseDecimal(item.allocation), 0);

  const overBudgetCount = budget.items.filter(
    item => !item.snoozed && item.categoryType === 'EXPENSE' && parseDecimal(item.spent) > parseDecimal(item.allocation),
  ).length;

  return (
    <div className="summary-panel">
      <div className="summary-card">
        <div className="summary-card-label">Total Assigned</div>
        <div className="summary-card-value">{formatCurrency(String(totalAssigned))}</div>
      </div>

      <div className="summary-card">
        <div className="summary-card-label">Total Spent</div>
        <div className="summary-card-value">{formatCurrency(budget.spent)}</div>
      </div>

      <div className="summary-card remaining">
        <div className="summary-card-label">Remaining</div>
        <div className="summary-card-value">{formatCurrency(budget.remaining)}</div>
      </div>

      {overBudgetCount > 0 && (
        <div className="summary-warning">
          <span>⚠️ {overBudgetCount} {overBudgetCount === 1 ? 'category' : 'categories'} over budget</span>
        </div>
      )}
    </div>
  );
}
