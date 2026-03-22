# Snooze Toggle for Budget Categories

## Overview

Add a snooze/wake toggle button to each expense row in the BudgetList. The backend already supports snooze (`snoozed` column on `budget_items`, API endpoint, frontend types, and dimmed row styling). This feature adds the missing UI control.

## Behavior

- Each expense row in BudgetList gets a toggle button in a new rightmost column.
- Active state: displays ⚡ emoji.
- Snoozed state: displays 💤 emoji. Row remains dimmed via existing `.snoozed` CSS (opacity 0.4, grayscale).
- Clicking the button calls `PUT /budgets/:year/:month/items/:itemId` with `{ snoozed: !current }`.
- Optimistic update: toggle the UI immediately, revert on error.
- `e.preventDefault()` + `e.stopPropagation()` on the button to prevent navigating into the transaction list.

## Scope

- Expense rows in BudgetList only (not income rows, not EditPlan page).
- No backend changes needed.

## Layout

### Desktop

- Add a 5th grid column to `.budget-row`: `1fr 120px 120px 100px 40px`.
- Add a matching empty ghost `<span>` in `.budget-list-columns` header.
- The toggle button sits in the new column.

### Mobile

- The toggle sits at the far right of the flex row.

## Button Style

Follows the existing inline action button pattern (reference: `edit-plan-item-btn` in EditPlan.css):

- `padding: 6px 14px`
- `border: 1px solid var(--color-border)`
- `border-radius: var(--radius-sm)`
- `font-size: 13px`
- `font-weight: 500`
- `background: transparent`
- `cursor: pointer`

## Components Affected

- `BudgetCategoryRow.tsx` — add toggle button, accept `onToggleSnooze` callback
- `BudgetCategoryRow.css` — update grid to 5 columns, style the button
- `BudgetList.tsx` — add ghost column header, pass snooze handler to rows, manage optimistic state
- `BudgetList.css` — update column header grid to match

## Data Flow

1. User clicks toggle on a row.
2. `BudgetList` optimistically updates the item's `snoozed` field in local state.
3. `BudgetList` calls `updateBudgetItem(year, month, itemId, { snoozed })`.
4. On success: state is already correct.
5. On error: revert the item's `snoozed` field and surface error.