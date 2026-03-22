import { formatCurrency } from '../../utils/format';
import type { EditableItem } from './types';

interface ExpenseRowProps {
  item: EditableItem;
  onStartEditing: (itemId: number) => void;
  onAmountChange: (itemId: number, value: string) => void;
  onSave: (itemId: number) => void;
  onCancel: (itemId: number) => void;
}

export function ExpenseRow({ item, onStartEditing, onAmountChange, onSave, onCancel }: ExpenseRowProps) {
  return (
    <div className="edit-plan-item-wrapper">
      <div className="edit-plan-item">
        <span className="edit-plan-item-name">{item.categoryName}</span>
        {item.editing ? (
          <form
            className="edit-plan-item-edit"
            onSubmit={e => { e.preventDefault(); onSave(item.id); }}
          >
            <input
              className="edit-plan-item-input"
              type="text"
              inputMode="numeric"
              value={item.displayAmount}
              onChange={e => onAmountChange(item.id, e.target.value)}
              autoFocus
            />
            <button
              type="submit"
              className="action-btn edit-plan-item-btn save"
              disabled={item.saving}
            >
              {item.saving ? '...' : 'Save'}
            </button>
            <button
              type="button"
              className="action-btn edit-plan-item-btn cancel"
              onClick={() => onCancel(item.id)}
            >
              Cancel
            </button>
          </form>
        ) : (
          <div className="edit-plan-item-display">
            <span className="edit-plan-item-amount">{formatCurrency(item.allocation)}</span>
            <button
              type="button"
              className="action-btn edit-plan-item-btn edit"
              onClick={() => onStartEditing(item.id)}
            >
              Edit
            </button>
          </div>
        )}
      </div>
      {item.saveError && <div className="edit-plan-item-error">{item.saveError}</div>}
    </div>
  );
}
