import { useState } from 'react';
import { Layout } from './components/Layout/Layout';
import { SettingsButton } from './components/SettingsButton/SettingsButton';
import { PeriodSelector } from './components/PeriodSelector/PeriodSelector';
import { SummaryPanel } from './components/SummaryPanel/SummaryPanel';
import { BudgetList } from './components/BudgetList/BudgetList';
import { useBudget } from './hooks/useBudget';

function App() {
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
      {budget && <BudgetList budget={budget} />}
    </Layout>
  );
}

export default App;
