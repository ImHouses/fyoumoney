import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { getBudget, getTransactions, deleteTransaction } from '../../api/budgetApi';
import type { TransactionResponse } from '../../types/budget';
import { TransactionRow } from './TransactionRow';
import './TransactionList.css';

export function TransactionList() {
  const { year, month, itemId } = useParams<{ year: string; month: string; itemId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const fromPlan = (location.state as { from?: string })?.from === 'plan';

  const [categoryName, setCategoryName] = useState<string>('');
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!year || !month || !itemId) return;

    const yearNum = Number(year);
    const monthNum = Number(month);
    const itemIdNum = Number(itemId);

    setLoading(true);
    setError(null);

    Promise.all([
      getBudget(yearNum, monthNum),
      getTransactions({ budgetItemId: itemIdNum }),
    ])
      .then(([budget, txns]) => {
        const item = budget.items.find(budgetItem => budgetItem.id === itemIdNum);
        setCategoryName(item?.categoryName ?? '');
        setTransactions(txns);
      })
      .catch(err => {
        setError(err instanceof Error ? err.message : 'Failed to load transactions');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [year, month, itemId]);

  const handleEdit = (id: number) => {
    navigate(`/transactions/${id}/edit`);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTransaction(id);
      setTransactions(prev => prev.filter(tx => tx.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete transaction');
    }
  };

  return (
    <div className="transaction-list-page">
      <div className="transaction-list-header">
        {fromPlan && (
          <button
            className="nav-btn"
            onClick={() => navigate('/')}
            aria-label="Go home"
          >
            ⌂
          </button>
        )}
        <button
          className="nav-btn"
          onClick={() => navigate(-1)}
          aria-label="Go back"
        >
          ‹
        </button>
        <h1 className="transaction-list-title">{categoryName || 'Transactions'}</h1>
      </div>

      {loading && (
        <div className="transaction-list-state">Loading...</div>
      )}

      {!loading && error && (
        <div className="transaction-list-state transaction-list-error">{error}</div>
      )}

      {!loading && !error && transactions.length === 0 && (
        <div className="transaction-list-state transaction-list-empty">
          No transactions yet.
        </div>
      )}

      {!loading && !error && transactions.length > 0 && (
        <div className="transaction-list-rows">
          {transactions.map(tx => (
            <TransactionRow
              key={tx.id}
              transaction={tx}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
