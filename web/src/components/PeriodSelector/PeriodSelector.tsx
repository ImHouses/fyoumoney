import { formatPeriod } from '../../utils/format';
import './PeriodSelector.css';

interface PeriodSelectorProps {
  year: number;
  month: number;
  onPrev: () => void;
  onNext: () => void;
}

export function PeriodSelector({ year, month, onPrev, onNext }: PeriodSelectorProps) {
  return (
    <div className="period-selector">
      <button className="period-selector-btn" onClick={onPrev} aria-label="Previous month">
        ‹
      </button>
      <span className="period-selector-label">{formatPeriod(year, month)}</span>
      <button className="period-selector-btn" onClick={onNext} aria-label="Next month">
        ›
      </button>
    </div>
  );
}
