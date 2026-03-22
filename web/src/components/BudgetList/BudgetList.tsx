import { Link } from 'react-router-dom';
import type { BudgetResponse } from '../../types/budget';
import { formatPeriod } from '../../utils/format';
import { BudgetCategoryRow } from '../BudgetCategoryRow/BudgetCategoryRow';
import './BudgetList.css';

interface BudgetListProps {
  budget: BudgetResponse;
  year: number;
  month: number;
  onPrev: () => void;
  onNext: () => void;
}

export function BudgetList({ budget, year, month, onPrev, onNext }: BudgetListProps) {
  const sorted = [...budget.items]
    .filter(item => item.categoryType === 'EXPENSE')
    .sort((a, b) => {
      if (a.snoozed !== b.snoozed) return a.snoozed ? 1 : -1;
      return a.categoryName.localeCompare(b.categoryName);
    });

  return (
    <div>
      <div className="budget-list-header">
        <div className="budget-list-header-top">
          <div>
            <h1 className="budget-list-title">Budget List</h1>
            <p className="budget-list-subtitle">{formatPeriod(year, month)}</p>
          </div>
          <Link to="/transactions/new" className="budget-list-create-btn">+ Create</Link>
        </div>
        <div className="budget-list-period-mobile">
          <button className="period-nav-btn" onClick={onPrev} aria-label="Previous month">‹</button>
          <span className="period-nav-label">{formatPeriod(year, month)}</span>
          <button className="period-nav-btn" onClick={onNext} aria-label="Next month">›</button>
          <Link to="/transactions/new" className="budget-list-create-btn mobile">+ Create</Link>
        </div>
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
