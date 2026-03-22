# Snooze Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a snooze/wake toggle button to each expense row in the BudgetList.

**Architecture:** The backend already has full snooze support (schema, models, API, types). This plan adds the UI toggle button to `BudgetCategoryRow`, introduces local state in `BudgetList` for optimistic updates, and adjusts the CSS grid to accommodate a 5th column.

**Tech Stack:** React, TypeScript, CSS

**Spec:** `docs/superpowers/specs/2026-03-22-snooze-toggle-design.md`

---

### File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `web/src/components/BudgetCategoryRow/BudgetCategoryRow.tsx` | Modify | Add toggle button, accept `onToggleSnooze` callback |
| `web/src/components/BudgetCategoryRow/BudgetCategoryRow.css` | Modify | 5-column grid, toggle button styles |
| `web/src/components/BudgetList/BudgetList.tsx` | Modify | Local state for items, snooze handler with optimistic update, ghost column header |
| `web/src/components/BudgetList/BudgetList.css` | Modify | 5-column grid on column headers |

---

### Task 1: Update CSS grids to 5 columns

**Files:**
- Modify: `web/src/components/BudgetCategoryRow/BudgetCategoryRow.css:1-3`
- Modify: `web/src/components/BudgetList/BudgetList.css:87-89`

- [ ] **Step 1: Update `.budget-row` grid to 5 columns**

In `web/src/components/BudgetCategoryRow/BudgetCategoryRow.css`, change line 3:

```css
grid-template-columns: 1fr 120px 120px 100px 40px;
```

- [ ] **Step 2: Update `.budget-list-columns` grid to match**

In `web/src/components/BudgetList/BudgetList.css`, change line 89:

```css
grid-template-columns: 1fr 120px 120px 100px 40px;
```

- [ ] **Step 3: Add toggle button styles to BudgetCategoryRow.css**

Append before the `/* Mobile */` comment in `web/src/components/BudgetCategoryRow/BudgetCategoryRow.css`:

```css
.budget-row-snooze-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
  text-decoration: none;
}

.budget-row-snooze-btn:hover {
  background-color: var(--color-bg-card-hover);
}
```

- [ ] **Step 4: Verify the app renders without errors**

Run: Open the app in the browser, check the BudgetList page loads. The grid should show a small empty 5th column. No visual breakage.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/BudgetCategoryRow/BudgetCategoryRow.css web/src/components/BudgetList/BudgetList.css
git commit -m "style: add 5th grid column and snooze button styles for budget rows"
```

---

### Task 2: Add toggle button to BudgetCategoryRow

**Files:**
- Modify: `web/src/components/BudgetCategoryRow/BudgetCategoryRow.tsx`

- [ ] **Step 1: Add `onToggleSnooze` prop to the interface**

```typescript
interface BudgetCategoryRowProps {
  item: BudgetItemResponse;
  year: number;
  month: number;
  onToggleSnooze?: (itemId: number) => void;
}
```

- [ ] **Step 2: Accept the prop and add toggle button**

Update the function signature:

```typescript
export function BudgetCategoryRow({ item, year, month, onToggleSnooze }: BudgetCategoryRowProps) {
```

Add the toggle button inside the `<Link>`, after the mobile compact view div and before the progress bar. Since `BudgetList` already filters to EXPENSE items only, no income guard is needed:

```tsx
<button
  className="budget-row-snooze-btn"
  onClick={e => {
    e.preventDefault();
    e.stopPropagation();
    onToggleSnooze?.(item.id);
  }}
  aria-label={item.snoozed ? 'Wake category' : 'Snooze category'}
  aria-pressed={item.snoozed}
>
  {item.snoozed ? '💤' : '⚡'}
</button>
```

- [ ] **Step 3: Verify the button renders**

Open the app. Each expense row should show ⚡ in the rightmost column. Clicking it should not navigate away (propagation stopped). Nothing happens yet since the handler isn't wired.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/BudgetCategoryRow/BudgetCategoryRow.tsx
git commit -m "feat: add snooze toggle button to BudgetCategoryRow"
```

---

### Task 3: Add ghost column header to BudgetList

**Files:**
- Modify: `web/src/components/BudgetList/BudgetList.tsx`

- [ ] **Step 1: Add empty span to column headers**

In `BudgetList.tsx`, update the `.budget-list-columns` div (line 47-52):

```tsx
<div className="budget-list-columns">
  <span>Category</span>
  <span>Assigned</span>
  <span>Spent</span>
  <span>Remaining</span>
  <span />
</div>
```

- [ ] **Step 2: Verify column alignment**

Open the app. The column headers should still align with the row data. The 5th column is empty in the header.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/BudgetList/BudgetList.tsx
git commit -m "feat: add ghost column header for snooze toggle"
```

---

### Task 4: Wire up optimistic snooze toggle in BudgetList

**Files:**
- Modify: `web/src/components/BudgetList/BudgetList.tsx`

- [ ] **Step 1: Add local state and imports**

Add `useState` and `useEffect` to the React import. Add `updateBudgetItem` to the API import. Add `BudgetItemResponse` to the type import:

```typescript
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import type { BudgetItemResponse, BudgetResponse } from '../../types/budget';
import { updateBudgetItem } from '../../api/budgetApi';
import { formatPeriod } from '../../utils/format';
import { BudgetCategoryRow } from '../BudgetCategoryRow/BudgetCategoryRow';
import './BudgetList.css';
```

- [ ] **Step 2: Add local items state with useEffect sync**

Inside the component, before the `sorted` variable, add local state that syncs when the budget changes:

```typescript
const [items, setItems] = useState<BudgetItemResponse[]>(budget.items);

useEffect(() => {
  setItems(budget.items);
}, [budget.id]);
```

This resets local state whenever the budget entity changes (different period). After a snooze toggle, if the parent re-fetches the same budget, the server now has the correct value so the reset is harmless.

- [ ] **Step 3: Add the snooze toggle handler**

First, add a helper function to update snoozed state for a given item. Place this before the component function:

```typescript
function setSnoozed(items: BudgetItemResponse[], itemId: number, snoozed: boolean): BudgetItemResponse[] {
  return items.map(item => (item.id === itemId ? { ...item, snoozed } : item));
}
```

Then, inside the component:

```typescript
const handleToggleSnooze = async (itemId: number) => {
  const target = items.find(item => item.id === itemId);
  if (!target) return;

  const newSnoozed = !target.snoozed;

  // Optimistic update
  setItems(prev => setSnoozed(prev, itemId, newSnoozed));

  try {
    await updateBudgetItem(year, month, itemId, { snoozed: newSnoozed });
  } catch (err) {
    // Revert on error
    setItems(prev => setSnoozed(prev, itemId, !newSnoozed));
    console.error('Failed to toggle snooze:', err);
  }
};
```

- [ ] **Step 4: Update sorted to use local items instead of budget.items**

Change the `sorted` variable:

```typescript
const sorted = [...items]
  .filter(item => item.categoryType === 'EXPENSE')
  .sort((a, b) => {
    if (a.snoozed !== b.snoozed) return a.snoozed ? 1 : -1;
    return a.categoryName.localeCompare(b.categoryName);
  });
```

- [ ] **Step 5: Pass the handler to BudgetCategoryRow**

Update the row rendering:

```tsx
sorted.map(item => (
  <BudgetCategoryRow
    key={item.id}
    item={item}
    year={year}
    month={month}
    onToggleSnooze={handleToggleSnooze}
  />
))
```

- [ ] **Step 6: Test the full flow**

Open the app:
1. Click ⚡ on an active expense row → row dims, emoji changes to 💤, row moves to bottom.
2. Click 💤 on a snoozed row → row un-dims, emoji changes to ⚡, row moves back to alphabetical position.
3. Refresh the page → snoozed state persists (server saved it).
4. Navigate to a different month and back → state is correct.

- [ ] **Step 7: Commit**

```bash
git add web/src/components/BudgetList/BudgetList.tsx
git commit -m "feat: wire up optimistic snooze toggle in BudgetList"
```