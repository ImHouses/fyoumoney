import type { BudgetItemResponse } from '../../types/budget';

export interface EditableItem extends BudgetItemResponse {
  editing: boolean;
  displayAmount: string;
  saving: boolean;
  saveError: string | null;
}

export function updateItemById(
  items: EditableItem[],
  itemId: number,
  updates: Partial<EditableItem>,
): EditableItem[] {
  return items.map(item => (item.id === itemId ? { ...item, ...updates } : item));
}
