import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { getBudget, updateBudgetItem } from '../../api/budgetApi';
import { formatCurrency, formatAmountDisplay, toRawAmount, formatPeriod } from '../../utils/format';
import { ExpenseRow } from './ExpenseRow';
import { updateItemById } from './types';
import type { EditableItem } from './types';
import './EditPlan.css';

export function EditPlan() {
  const navigate = useNavigate();
  const { year: yearParam, month: monthParam } = useParams();
  const year = Number(yearParam);
  const month = Number(monthParam);

  const [items, setItems] = useState<EditableItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    getBudget(year, month)
      .then(budget => {
        setItems(
          budget.items.map(item => ({
            ...item,
            editing: false,
            displayAmount: '',
            saving: false,
            saveError: null,
          })),
        );
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load budget'))
      .finally(() => setLoading(false));
  }, [year, month]);

  const incomeItems = items.filter(item => item.categoryType === 'INCOME');
  const expenseItems = items.filter(item => item.categoryType === 'EXPENSE');

  const totalIncome = incomeItems.reduce((sum, item) => sum + parseFloat(item.spent), 0);

  const startEditing = useCallback((itemId: number) => {
    setItems(prev => {
      const target = prev.find(item => item.id === itemId);
      if (!target) return prev;
      const displayAmount = formatAmountDisplay(target.allocation.replace('.', ''));
      return updateItemById(prev, itemId, { editing: true, displayAmount });
    });
  }, []);

  const handleAmountChange = useCallback((itemId: number, value: string) => {
    setItems(prev => updateItemById(prev, itemId, { displayAmount: formatAmountDisplay(value) }));
  }, []);

  const saveItem = useCallback(async (itemId: number) => {
    let newAllocation = '';
    setItems(prev => {
      const target = prev.find(item => item.id === itemId);
      if (target) newAllocation = toRawAmount(target.displayAmount);
      return updateItemById(prev, itemId, { saving: true });
    });

    if (!newAllocation) return;

    try {
      await updateBudgetItem(year, month, itemId, { allocation: newAllocation });
      setItems(prev =>
        updateItemById(prev, itemId, { allocation: newAllocation, editing: false, saving: false, saveError: null }),
      );
    } catch (err) {
      const saveError = err instanceof Error ? err.message : 'Failed to save';
      setItems(prev => updateItemById(prev, itemId, { saving: false, saveError }));
    }
  }, [year, month]);

  const cancelEditing = useCallback((itemId: number) => {
    setItems(prev => updateItemById(prev, itemId, { editing: false }));
  }, []);

  return (
    <div className="edit-plan-page">
      <div className="edit-plan-header">
        <button className="edit-plan-back" onClick={() => navigate(-1)} aria-label="Go back">
          ‹
        </button>
        <h1 className="edit-plan-title">My Plan</h1>
      </div>

      <p className="edit-plan-period">{formatPeriod(year, month)}</p>

      {loading && <div className="edit-plan-status">Loading...</div>}
      {error && <div className="edit-plan-error">{error}</div>}

      {!loading && !error && (
        <>
          <section className="edit-plan-section">
            <h2 className="edit-plan-section-title">Income</h2>
            <p className="edit-plan-summary">
              You have registered a total of <strong>{formatCurrency(String(totalIncome))}</strong> in income this month from:
            </p>
            <div className="edit-plan-items">
              {incomeItems.length === 0 ? (
                <div className="edit-plan-empty">No income recorded this month</div>
              ) : (
                incomeItems.map(item => (
                  <Link
                    key={item.id}
                    to={`/budgets/${year}/${month}/items/${item.id}/transactions`}
                    className="edit-plan-item edit-plan-item-link"
                  >
                    <span className="edit-plan-item-name">{item.categoryName}</span>
                    <span className="edit-plan-item-amount">{formatCurrency(item.spent)}</span>
                  </Link>
                ))
              )}
            </div>
          </section>

          <section className="edit-plan-section">
            <h2 className="edit-plan-section-title">Expenses</h2>
            <div className="edit-plan-items">
              {expenseItems.length === 0 ? (
                <div className="edit-plan-empty">No expense categories</div>
              ) : (
                expenseItems.map(item => (
                  <ExpenseRow
                    key={item.id}
                    item={item}
                    onStartEditing={startEditing}
                    onAmountChange={handleAmountChange}
                    onSave={saveItem}
                    onCancel={cancelEditing}
                  />
                ))
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
