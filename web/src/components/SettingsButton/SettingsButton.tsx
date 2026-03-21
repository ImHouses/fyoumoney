import { useTheme } from '../../theme/ThemeContext';

export function SettingsButton() {
  const { toggle } = useTheme();

  return (
    <button
      onClick={toggle}
      aria-label="Toggle theme"
      style={{
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        fontSize: '18px',
        padding: '4px',
        lineHeight: 1,
      }}
    >
      ⚙️
    </button>
  );
}
