import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout/Layout';
import { SettingsButton } from './components/SettingsButton/SettingsButton';
import { PeriodSelector } from './components/PeriodSelector/PeriodSelector';
import { SummaryPanel } from './components/SummaryPanel/SummaryPanel';
import { BudgetList } from './components/BudgetList/BudgetList';
import { TransactionForm } from './components/TransactionForm/TransactionForm';
import { SetupPage } from './components/SetupPage/SetupPage';
import { useBudget } from './hooks/useBudget';
import { useCategories } from './hooks/useCategories';

function BudgetPage() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const { budget, loading, error } = useBudget(year, month);

  const prevMonth = () => {
    if (month === 1) {
      setMonth(12);
      setYear(y => y - 1);
    } else {
      setMonth(m => m - 1);
    }
  };

  const nextMonth = () => {
    if (month === 12) {
      setMonth(1);
      setYear(y => y + 1);
    } else {
      setMonth(m => m + 1);
    }
  };

  const sidebar = (
    <>
      <div className="sidebar-header">
        <span className="sidebar-logo">fumoney</span>
        <SettingsButton />
      </div>
      <div className="sidebar-label">Period</div>
      <PeriodSelector year={year} month={month} onPrev={prevMonth} onNext={nextMonth} />
      {budget && (
        <>
          <div className="sidebar-label">Summary</div>
          <SummaryPanel budget={budget} />
        </>
      )}
    </>
  );

  return (
    <Layout sidebar={sidebar}>
      {loading && <div style={{ color: 'var(--color-text-muted)', padding: '40px 0' }}>Loading...</div>}
      {error && <div style={{ color: 'var(--color-warning-text)', padding: '40px 0' }}>{error}</div>}
      {budget && (
        <BudgetList
          budget={budget}
          year={year}
          month={month}
          onPrev={prevMonth}
          onNext={nextMonth}
        />
      )}
    </Layout>
  );
}

function RootRedirect() {
  const { categories, loading } = useCategories();

  if (loading) return null;
  if (categories.length === 0) return <Navigate to="/setup" replace />;
  return <BudgetPage />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/setup" element={<SetupPage />} />
        <Route path="/transactions/new" element={<TransactionForm />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;