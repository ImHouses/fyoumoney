import type {
  CategoryRequest,
  CategoryResponse,
  BudgetResponse,
  BudgetSummaryResponse,
  BudgetItemUpdateRequest,
  TransactionRequest,
  TransactionResponse,
} from '../types/budget';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!res.ok) {
    const body = await res.text();
    let message: string;
    try {
      message = JSON.parse(body).error ?? body;
    } catch {
      message = body;
    }
    throw new Error(message || `Request failed: ${res.status}`);
  }

  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

// Categories
export const getCategories = () =>
  request<CategoryResponse[]>('/categories');

export const getCategory = (id: number) =>
  request<CategoryResponse>(`/categories/${id}`);

export const createCategory = (body: CategoryRequest) =>
  request<number>('/categories', { method: 'POST', body: JSON.stringify(body) });

export const createCategories = (body: CategoryRequest[]) =>
  request<number[]>('/categories/batch', { method: 'POST', body: JSON.stringify(body) });

export const updateCategory = (id: number, body: Partial<CategoryRequest>) =>
  request<CategoryResponse>(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(body) });

export const deleteCategory = (id: number) =>
  request<void>(`/categories/${id}`, { method: 'DELETE' });

// Budgets
export const listBudgets = (year?: number) =>
  request<BudgetSummaryResponse[]>(`/budgets${year != null ? `?year=${year}` : ''}`);

export const getBudget = (year: number, month: number) =>
  request<BudgetResponse>(`/budgets/${year}/${month}`);

export const updateBudgetItem = (
  year: number,
  month: number,
  itemId: number,
  body: BudgetItemUpdateRequest,
) =>
  request<void>(`/budgets/${year}/${month}/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });

// Transactions
export const getTransactions = (params?: {
  categoryId?: number;
  year?: number;
  month?: number;
  budgetItemId?: number;
}) => {
  const qs = new URLSearchParams();
  if (params?.categoryId != null) qs.set('categoryId', String(params.categoryId));
  if (params?.year != null) qs.set('year', String(params.year));
  if (params?.month != null) qs.set('month', String(params.month));
  if (params?.budgetItemId != null) qs.set('budgetItemId', String(params.budgetItemId));
  const query = qs.toString();
  return request<TransactionResponse[]>(`/transactions${query ? `?${query}` : ''}`);
};

export const getTransaction = (id: number) =>
  request<TransactionResponse>(`/transactions/${id}`);

export const createTransaction = (body: TransactionRequest) =>
  request<number>('/transactions', { method: 'POST', body: JSON.stringify(body) });

export const updateTransaction = (id: number, body: Partial<TransactionRequest>) =>
  request<void>(`/transactions/${id}`, { method: 'PUT', body: JSON.stringify(body) });

export const deleteTransaction = (id: number) =>
  request<void>(`/transactions/${id}`, { method: 'DELETE' });
