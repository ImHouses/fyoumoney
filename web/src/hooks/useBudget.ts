import { useState, useEffect, useCallback } from 'react';
import type { BudgetResponse } from '../types/budget';
import { getBudget } from '../api/budgetApi';

interface UseBudgetResult {
  budget: BudgetResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useBudget(year: number, month: number): UseBudgetResult {
  const [budget, setBudget] = useState<BudgetResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBudget = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getBudget(year, month);
      setBudget(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load budget');
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  useEffect(() => {
    fetchBudget();
  }, [fetchBudget]);

  return { budget, loading, error, refetch: fetchBudget };
}
