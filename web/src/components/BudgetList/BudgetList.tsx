import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import type { BudgetItemResponse, BudgetResponse } from '../../types/budget';
import { updateBudgetItem } from '../../api/budgetApi';
import { formatPeriod } from '../../utils/format';
import { BudgetCategoryRow } from '../BudgetCategoryRow/BudgetCategoryRow';
import './BudgetList.css';

interface BudgetListProps {
  budget: BudgetResponse;
  year: number;
  month: number;
  onPrev: () => void;
  onNext: () => void;
  onRefresh: () => void;
}

function setSnoozed(items: BudgetItemResponse[], itemId: number, snoozed: boolean): BudgetItemResponse[] {
  return items.map(item => (item.id === itemId ? { ...item, snoozed } : item));
}

export function BudgetList({ budget, year, month, onPrev, onNext, onRefresh }: BudgetListProps) {
  const [items, setItems] = useState<BudgetItemResponse[]>(budget.items);

  useEffect(() => {
    setItems(budget.items);
  }, [budget.id, budget.items]);

  const handleToggleSnooze = async (itemId: number) => {
    const target = items.find(item => item.id === itemId);
    if (!target) return;

    const newSnoozed = !target.snoozed;

    // Optimistic update
    setItems(prev => setSnoozed(prev, itemId, newSnoozed));

    try {
      await updateBudgetItem(year, month, itemId, { snoozed: newSnoozed });
      onRefresh();
    } catch (err) {
      // Revert on error
      setItems(prev => setSnoozed(prev, itemId, !newSnoozed));
      console.error('Failed to toggle snooze:', err);
    }
  };

  const sorted = [...items]
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
            <h1 className="budget-list-title">My Budget</h1>
            <p className="budget-list-subtitle">{formatPeriod(year, month)}</p>
          </div>
          <div className="budget-list-header-actions">
            <Link to={`/budgets/${year}/${month}/edit`} className="budget-list-create-btn">Edit Plan</Link>
            <Link to="/transactions/new" className="budget-list-create-btn">+ Create</Link>
          </div>
        </div>
        <div className="budget-list-period-mobile">
          <button className="period-nav-btn" onClick={onPrev} aria-label="Previous month">‹</button>
          <span className="period-nav-label">{formatPeriod(year, month)}</span>
          <button className="period-nav-btn" onClick={onNext} aria-label="Next month">›</button>
          <div className="budget-list-mobile-actions">
            <Link to={`/budgets/${year}/${month}/edit`} className="budget-list-create-btn mobile">Edit Plan</Link>
            <Link to="/transactions/new" className="budget-list-create-btn mobile">+ Create</Link>
          </div>
        </div>
      </div>

      <div className="budget-list-columns">
        <span>Category</span>
        <span>Assigned</span>
        <span>Spent</span>
        <span>Remaining</span>
        <span />
      </div>

      <div className="budget-list-items">
        {sorted.length === 0 ? (
          <div className="budget-list-empty">No budget items for this period.</div>
        ) : (
          sorted.map(item => (
            <BudgetCategoryRow
              key={item.id}
              item={item}
              year={year}
              month={month}
              onToggleSnooze={handleToggleSnooze}
            />
          ))
        )}
      </div>
    </div>
  );
}
