import type { BudgetResponse } from '../../types/budget';
import { formatPeriod } from '../../utils/format';
import { BudgetCategoryRow } from '../BudgetCategoryRow/BudgetCategoryRow';
import './BudgetList.css';

interface BudgetListProps {
  budget: BudgetResponse;
}

export function BudgetList({ budget }: BudgetListProps) {
  const sorted = [...budget.items].sort((a, b) => {
    if (a.snoozed !== b.snoozed) return a.snoozed ? 1 : -1;
    return a.categoryName.localeCompare(b.categoryName);
  });

  return (
    <div>
      <div className="budget-list-header">
        <h1 className="budget-list-title">Budget List</h1>
        <p className="budget-list-subtitle">{formatPeriod(budget.year, budget.month)}</p>
      </div>

      <div className="budget-list-columns">
        <span>Category</span>
        <span>Assigned</span>
        <span>Spent</span>
        <span>Remaining</span>
      </div>

      <div className="budget-list-items">
        {sorted.length === 0 ? (
          <div className="budget-list-empty">No budget items for this period.</div>
        ) : (
          sorted.map(item => <BudgetCategoryRow key={item.id} item={item} />)
        )}
      </div>
    </div>
  );
}
