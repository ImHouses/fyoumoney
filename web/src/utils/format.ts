const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

export function formatCurrency(value: string): string {
  const num = parseFloat(value);
  if (isNaN(num)) return '$0';
  // Drop .00 but keep other decimals
  const formatted = currencyFormatter.format(num);
  return formatted.replace(/\.00$/, '');
}

export function parseDecimal(value: string): number {
  const num = parseFloat(value);
  return isNaN(num) ? 0 : num;
}

export function getProgressColor(
  allocation: string,
  spent: string,
): 'green' | 'yellow' | 'red' | 'gray' {
  const alloc = parseDecimal(allocation);
  const sp = parseDecimal(spent);
  if (alloc === 0) return 'gray';
  const ratio = sp / alloc;
  if (ratio >= 1) return 'gray';
  if (ratio >= 0.8) return 'red';
  if (ratio >= 0.5) return 'yellow';
  return 'green';
}

export function getProgressWidth(allocation: string, spent: string): number {
  const alloc = parseDecimal(allocation);
  const sp = parseDecimal(spent);
  if (alloc === 0) return 0;
  return Math.min((sp / alloc) * 100, 100);
}

const emojiMap: Record<string, string> = {
  groceries: '🛒',
  food: '🛒',
  transportation: '🚗',
  transport: '🚗',
  housing: '🏠',
  rent: '🏠',
  mortgage: '🏠',
  dining: '🍽️',
  restaurants: '🍽️',
  utilities: '⚡',
  entertainment: '🎵',
  internet: '📶',
  phone: '📱',
  health: '🏥',
  savings: '💰',
  income: '💰',
  salary: '💼',
  freelance: '💼',
  subscriptions: '📺',
  pets: '🐾',
  gym: '🏋️',
  fitness: '🏋️',
  travel: '✈️',
  gifts: '🎁',
};

export function getCategoryEmoji(name: string): string {
  const lower = name.toLowerCase();
  for (const [key, emoji] of Object.entries(emojiMap)) {
    if (lower.includes(key)) return emoji;
  }
  return '📦';
}

const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export function formatPeriod(year: number, month: number): string {
  return `${monthNames[month - 1]} ${year}`;
}
