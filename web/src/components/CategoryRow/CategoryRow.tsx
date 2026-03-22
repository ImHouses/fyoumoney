import { useState, useCallback } from 'react';
import type { TransactionType } from '../../types/budget';
import { formatCurrency, getCategoryEmoji, formatAmountDisplay, toRawAmount, parseDecimal } from '../../utils/format';
import './CategoryRow.css';

export interface LocalCategory {
  tempId: string;
  name: string;
  type: TransactionType;
  defaultAllocation: string;
  editing: boolean;
  isNew: boolean;
}

interface CategoryRowProps {
  category: LocalCategory;
  onSave: (updated: LocalCategory) => void;
  onDelete: (tempId: string) => void;
  onCancel: (tempId: string) => void;
}

export function CategoryRow({ category, onSave, onDelete, onCancel }: CategoryRowProps) {
  const [name, setName] = useState(category.name);
  const [displayAllocation, setDisplayAllocation] = useState(
    category.defaultAllocation
      ? formatAmountDisplay(String(Math.round(parseDecimal(category.defaultAllocation) * 100)))
      : '',
  );
  const [rawAllocation, setRawAllocation] = useState(category.defaultAllocation);

  const handleAllocationChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target.value;
    setDisplayAllocation(formatAmountDisplay(input));
    setRawAllocation(toRawAmount(input));
  }, []);

  const isIncome = category.type === 'INCOME';

  const handleSave = () => {
    if (!name.trim()) return;
    if (!isIncome && !rawAllocation) return;
    onSave({
      ...category,
      name: name.trim(),
      defaultAllocation: isIncome ? '0' : rawAllocation,
      editing: false,
      isNew: false,
    });
  };

  const handleCancel = () => {
    onCancel(category.tempId);
  };

  if (category.editing) {
    return (
      <form className="category-row-edit" onSubmit={e => { e.preventDefault(); handleSave(); }}>
        <div className="category-row-edit-fields">
          <input
            type="text"
            placeholder="Category name"
            value={name}
            onChange={e => setName(e.target.value)}
            autoFocus
          />
          {!isIncome && (
            <input
              type="text"
              inputMode="numeric"
              placeholder="$0.00"
              value={displayAllocation}
              onChange={handleAllocationChange}
            />
          )}
        </div>
        <div className="category-row-edit-actions">
          <button type="button" className="category-row-edit-btn cancel" onClick={handleCancel}>Cancel</button>
          <button type="submit" className="category-row-edit-btn save">Save</button>
        </div>
      </form>
    );
  }

  return (
    <div className="category-row">
      <span className="category-row-emoji">{getCategoryEmoji(category.name)}</span>
      <span className="category-row-name">{category.name}</span>
      {!isIncome && <span className="category-row-allocation">{formatCurrency(category.defaultAllocation)}</span>}
      <div className="category-row-actions">
        <button
          className="category-row-btn"
          onClick={() => onSave({ ...category, editing: true })}
          aria-label="Edit"
        >
          ✏️
        </button>
        <button
          className="category-row-btn delete"
          onClick={() => onDelete(category.tempId)}
          aria-label="Delete"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
