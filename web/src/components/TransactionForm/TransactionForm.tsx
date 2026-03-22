import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getCategories, createTransaction } from '../../api/budgetApi';
import type { CategoryResponse } from '../../types/budget';
import { formatAmountDisplay, toRawAmount } from '../../utils/format';
import './TransactionForm.css';

export function TransactionForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [displayAmount, setDisplayAmount] = useState('');
  const [rawAmount, setRawAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    const catParam = searchParams.get('categoryId');
    if (catParam) setCategoryId(catParam);
  }, [searchParams]);

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

    try {
      await createTransaction({
        amount: rawAmount,
        categoryId: Number(categoryId),
        description,
        date,
      });
      navigate(-1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create transaction');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="transaction-form-page">
      <div className="transaction-form-header">
        <button className="transaction-form-back" onClick={() => navigate(-1)} aria-label="Go back">
          ‹
        </button>
        <h1 className="transaction-form-title">New Transaction</h1>
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
          >
            <option value="">Select a category</option>
            {categories.map(cat => (
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
            required
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
          {submitting ? 'Creating...' : 'Create Transaction'}
        </button>
      </form>
    </div>
  );
}