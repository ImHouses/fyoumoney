import { useState, useEffect, useRef } from 'react';
import type { TransactionResponse } from '../../types/budget';
import { formatCurrency } from '../../utils/format';

interface TransactionRowProps {
  transaction: TransactionResponse;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
}

export function TransactionRow({ transaction, onEdit, onDelete }: TransactionRowProps) {
  const [confirming, setConfirming] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearConfirm = () => {
    setConfirming(false);
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const handleDeleteClick = () => {
    if (!confirming) {
      setConfirming(true);
      timeoutRef.current = setTimeout(clearConfirm, 3000);
    } else {
      clearConfirm();
      onDelete(transaction.id);
    }
  };

  const handleDeleteBlur = () => {
    clearConfirm();
  };

  const formattedDate = new Date(transaction.date + 'T00:00:00').toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });

  return (
    <div className="transaction-row">
      <div className="transaction-row-main">
        <span className="transaction-row-date">{formattedDate}</span>
        <span className="transaction-row-description">
          {transaction.description || <span className="transaction-row-no-description">No description</span>}
        </span>
      </div>
      <div className="transaction-row-right">
        <span className="transaction-row-amount">{formatCurrency(transaction.amount)}</span>
        <div className="transaction-row-actions">
          <button
            className="transaction-row-btn transaction-row-edit"
            onClick={() => onEdit(transaction.id)}
            type="button"
          >
            Edit
          </button>
          <button
            className={`transaction-row-btn transaction-row-delete${confirming ? ' confirming' : ''}`}
            onClick={handleDeleteClick}
            onBlur={handleDeleteBlur}
            type="button"
          >
            {confirming ? 'Are you sure?' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}
