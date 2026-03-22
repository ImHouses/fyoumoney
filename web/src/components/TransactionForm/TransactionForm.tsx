import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams, useParams } from 'react-router-dom';
import { getCategories, getTransaction, createTransaction, updateTransaction } from '../../api/budgetApi';
import type { CategoryResponse, TransactionType } from '../../types/budget';
import { formatAmountDisplay, toRawAmount } from '../../utils/format';
import './TransactionForm.css';

export function TransactionForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { id: editId } = useParams<{ id: string }>();
  const isEditing = editId != null;

  const [allCategories, setAllCategories] = useState<CategoryResponse[]>([]);
  const [transactionType, setTransactionType] = useState<TransactionType>('EXPENSE');
  const [displayAmount, setDisplayAmount] = useState('');
  const [rawAmount, setRawAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(isEditing);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCategories().then(setAllCategories).catch(() => {});
  }, []);

  useEffect(() => {
    if (!isEditing) {
      const catParam = searchParams.get('categoryId');
      if (catParam) setCategoryId(catParam);
    }
  }, [searchParams, isEditing]);

  useEffect(() => {
    if (!isEditing) return;

    setLoading(true);
    getTransaction(Number(editId))
      .then(tx => {
        setRawAmount(tx.amount);
        setDisplayAmount(formatAmountDisplay(tx.amount.replace('.', '')));
        setCategoryId(String(tx.categoryId));
        setDescription(tx.description);
        setDate(tx.date);
        setTransactionType(tx.type);
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load transaction'))
      .finally(() => setLoading(false));
  }, [editId, isEditing]);

  const filteredCategories = allCategories.filter(cat => cat.type === transactionType);

  const handleTypeChange = (type: TransactionType) => {
    if (isEditing) return;
    setTransactionType(type);
    setCategoryId('');
  };

  const handleAmountChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target.value;
    const formatted = formatAmountDisplay(input);
    setDisplayAmount(formatted);
    setRawAmount(toRawAmount(input));
  }, []);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    const body = {
      amount: rawAmount,
      categoryId: Number(categoryId),
      description,
      date,
    };

    try {
      if (isEditing) {
        await updateTransaction(Number(editId), body);
      } else {
        await createTransaction(body);
      }
      navigate(-1);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${isEditing ? 'update' : 'create'} transaction`);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="transaction-form-page">
        <div className="transaction-form-header">
          <button className="transaction-form-back" onClick={() => navigate(-1)} aria-label="Go back">‹</button>
          <h1 className="transaction-form-title">{isEditing ? 'Edit Transaction' : 'New Transaction'}</h1>
        </div>
        <div style={{ color: 'var(--color-text-muted)', padding: '40px 0' }}>Loading...</div>
      </div>
    );
  }

  return (
    <div className="transaction-form-page">
      <div className="transaction-form-header">
        <button className="transaction-form-back" onClick={() => navigate(-1)} aria-label="Go back">
          ‹
        </button>
        <h1 className="transaction-form-title">{isEditing ? 'Edit Transaction' : 'New Transaction'}</h1>
      </div>

      <div className={`segmented-btn${isEditing ? ' disabled' : ''}`} role="radiogroup" aria-label="Transaction type">
        <button
          role="radio"
          aria-checked={transactionType === 'EXPENSE'}
          className={`segmented-btn-item${transactionType === 'EXPENSE' ? ' selected' : ''}`}
          onClick={() => handleTypeChange('EXPENSE')}
          type="button"
          disabled={isEditing}
        >
          Expense
        </button>
        <button
          role="radio"
          aria-checked={transactionType === 'INCOME'}
          className={`segmented-btn-item${transactionType === 'INCOME' ? ' selected' : ''}`}
          onClick={() => handleTypeChange('INCOME')}
          type="button"
          disabled={isEditing}
        >
          Income
        </button>
      </div>

      <form className="transaction-form" onSubmit={handleSubmit}>
        <div className="transaction-form-field">
          <label className="transaction-form-label" htmlFor="amount">Amount</label>
          <input
            id="amount"
            className="transaction-form-input"
            type="text"
            inputMode="numeric"
            placeholder="$0.00"
            value={displayAmount}
            onChange={handleAmountChange}
            required
          />
        </div>

        <div className="transaction-form-field">
          <label className="transaction-form-label" htmlFor="category">Category</label>
          <select
            id="category"
            className="transaction-form-select"
            value={categoryId}
            onChange={e => setCategoryId(e.target.value)}
            required
            disabled={isEditing}
          >
            <option value="">Select a category</option>
            {filteredCategories.map(cat => (
              <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
          </select>
        </div>

        <div className="transaction-form-field">
          <label className="transaction-form-label" htmlFor="description">Description</label>
          <input
            id="description"
            className="transaction-form-input"
            type="text"
            placeholder="What was this for?"
            value={description}
            onChange={e => setDescription(e.target.value)}
          />
        </div>

        <div className="transaction-form-field">
          <label className="transaction-form-label" htmlFor="date">Date</label>
          <input
            id="date"
            className="transaction-form-input"
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            required
          />
        </div>

        {error && <div className="transaction-form-error">{error}</div>}

        <button
          type="submit"
          className="transaction-form-submit"
          disabled={submitting}
        >
          {submitting
            ? (isEditing ? 'Updating...' : 'Creating...')
            : (isEditing ? 'Update Transaction' : 'Create Transaction')
          }
        </button>
      </form>
    </div>
  );
}
