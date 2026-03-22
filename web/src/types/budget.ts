export type TransactionType = 'INCOME' | 'EXPENSE';

export interface CategoryRequest {
  name: string;
  type: TransactionType;
  defaultAllocation: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  type: TransactionType;
  defaultAllocation: string;
  active: boolean;
}

export interface BudgetItemResponse {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryType: TransactionType;
  allocation: string;
  spent: string;
  snoozed: boolean;
}

export interface BudgetResponse {
  id: number;
  year: number;
  month: number;
  spent: string;
  remaining: string;
  items: BudgetItemResponse[];
}

export interface BudgetSummaryResponse {
  id: number;
  year: number;
  month: number;
}

export interface BudgetItemUpdateRequest {
  allocation?: string;
  snoozed?: boolean;
}

export interface TransactionRequest {
  amount: string;
  categoryId: number;
  description?: string;
  date: string;
}

export interface TransactionResponse {
  id: number;
  amount: string;
  type: TransactionType;
  description: string;
  date: string;
  budgetItemId: number;
  categoryId: number;
}

export interface ErrorResponse {
  error: string;
}
