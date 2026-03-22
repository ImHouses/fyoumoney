import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategories, createCategories } from '../../api/budgetApi';
import type { CategoryRequest } from '../../types/budget';
import { formatCurrency, parseDecimal } from '../../utils/format';
import { CategoryRow, type LocalCategory } from '../CategoryRow/CategoryRow';
import './SetupPage.css';

export function SetupPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<LocalCategory[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guardLoading, setGuardLoading] = useState(true);

  // Guard: redirect if categories already exist
  useEffect(() => {
    getCategories()
      .then(existing => {
        if (existing.length > 0) navigate('/', { replace: true });
        else setGuardLoading(false);
      })
      .catch(() => setGuardLoading(false));
  }, [navigate]);

  const incomeCategories = categories.filter(c => c.type === 'INCOME');
  const expenseCategories = categories.filter(c => c.type === 'EXPENSE');

  const totalIncome = incomeCategories
    .filter(c => !c.editing)
    .reduce((sum, c) => sum + parseDecimal(c.defaultAllocation), 0);
  const totalExpenses = expenseCategories
    .filter(c => !c.editing)
    .reduce((sum, c) => sum + parseDecimal(c.defaultAllocation), 0);
  const remaining = totalIncome - totalExpenses;

  const addCategory = (type: 'INCOME' | 'EXPENSE') => {
    setCategories(prev => [
      ...prev,
      {
        tempId: crypto.randomUUID(),
        name: '',
        type,
        defaultAllocation: '',
        editing: true,
        isNew: true,
      },
    ]);
  };

  const handleSave = (updated: LocalCategory) => {
    setCategories(prev => prev.map(c => (c.tempId === updated.tempId ? updated : c)));
  };

  const handleDelete = (tempId: string) => {
    setCategories(prev => prev.filter(c => c.tempId !== tempId));
  };

  const handleCancel = (tempId: string) => {
    setCategories(prev => {
      const cat = prev.find(c => c.tempId === tempId);
      if (cat?.isNew) return prev.filter(c => c.tempId !== tempId);
      return prev.map(c => (c.tempId === tempId ? { ...c, editing: false } : c));
    });
  };

  const hasIncome = incomeCategories.some(c => !c.editing && c.name);
  const hasExpense = expenseCategories.some(c => !c.editing && c.name);
  const canSubmit = hasIncome && hasExpense && !submitting;

  const handleDone = async () => {
    setError(null);
    setSubmitting(true);

    const saved = categories.filter(c => !c.editing && c.name);
    const requests: CategoryRequest[] = saved.map(c => ({
      name: c.name,
      type: c.type,
      defaultAllocation: c.defaultAllocation,
    }));

    try {
      await createCategories(requests);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save categories');
    } finally {
      setSubmitting(false);
    }
  };

  if (guardLoading) return null;

  return (
    <div className="setup-page">
      <div className="setup-header">
        <h1 className="setup-title">Before anything...</h1>
        <p className="setup-subtitle">
          Set up your budget plan for a typical month. Don't worry — you can customize your spending anytime!
        </p>
      </div>

      <div className="setup-section">
        <div className="setup-section-label">Income</div>
        <div className="setup-section-items">
          {incomeCategories.map(cat => (
            <CategoryRow
              key={`${cat.tempId}-${cat.editing}`}
              category={cat}
              onSave={handleSave}
              onDelete={handleDelete}
              onCancel={handleCancel}
            />
          ))}
        </div>
        <button className="setup-add-btn" onClick={() => addCategory('INCOME')}>
          + Add income source
        </button>
      </div>

      <div className="setup-section">
        <div className="setup-section-label">Expenses</div>
        <div className="setup-section-items">
          {expenseCategories.map(cat => (
            <CategoryRow
              key={`${cat.tempId}-${cat.editing}`}
              category={cat}
              onSave={handleSave}
              onDelete={handleDelete}
              onCancel={handleCancel}
            />
          ))}
        </div>
        <button className="setup-add-btn" onClick={() => addCategory('EXPENSE')}>
          + Add spending category
        </button>
      </div>

      {(hasIncome || hasExpense) && (
        <div className="setup-summary">
          <div className="setup-summary-row">
            <span className="setup-summary-label">Monthly income</span>
            <span className="setup-summary-value">{formatCurrency(String(totalIncome))}</span>
          </div>
          <div className="setup-summary-row">
            <span className="setup-summary-label">Monthly expenses</span>
            <span className="setup-summary-value">{formatCurrency(String(totalExpenses))}</span>
          </div>
          <div className="setup-summary-row">
            <span className="setup-summary-label">Remaining</span>
            <span className="setup-summary-value remaining">{formatCurrency(String(remaining))}</span>
          </div>
        </div>
      )}

      {error && <div className="setup-error">{error}</div>}

      <button className="setup-done-btn" disabled={!canSubmit} onClick={handleDone}>
        {submitting ? 'Saving...' : "I'm done, let's go!"}
      </button>
    </div>
  );
}
