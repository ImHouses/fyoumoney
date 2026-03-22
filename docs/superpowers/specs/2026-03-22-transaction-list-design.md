# Transaction List for Budget Items

## Goal

Allow users to view, edit, and delete transactions belonging to a specific budget item. Accessible from both the main budget page (expense items) and the Edit Plan page (income items).

## Backend

### Filter transactions by budget item

Add `budgetItemId` as an optional query parameter to `GET /transactions`, additive alongside existing `categoryId`/`year`/`month` params. This is more precise than `categoryId`+`year`+`month` since `budgetItemId` directly maps to `Transactions.budgetItemId` (no join needed).

**Files:**
- `TransactionRepository.kt` — add `budgetItemId: Int?` param to `findAll()`. When present, filter with `Transactions.budgetItemId eq budgetItemId` (direct column filter, no join required)
- `TransactionService.kt` — pass through `budgetItemId` param in `getAll()`
- `TransactionRoutes.kt` — read `budgetItemId` from query params, pass to service

## Frontend

### Transaction list page

**Route:** `/budgets/:year/:month/items/:itemId/transactions`

`itemId` in the URL is the budget item ID (`BudgetItemResponse.id`). Extract via `useParams()`.

**New component:** `web/src/components/TransactionList/TransactionList.tsx`

**Data flow:**
1. Extract `year`, `month`, `itemId` from URL params via `useParams()`
2. Fetch `getBudget(year, month)` to find the budget item by `itemId` — use `item.categoryName` for the page header title
3. Fetch `getTransactions({ budgetItemId: itemId })` to get the transaction list

**UI:**
- Header with back button and category name as title
- Loading, error, and empty states (consistent with existing pages)
- Each row shows: date, description, amount
- Action buttons per row: Edit, Delete

**`TransactionRow` component** (`TransactionList/TransactionRow.tsx`):
- Props: `transaction: TransactionResponse`, `onEdit: (id: number) => void`, `onDelete: (id: number) => void`
- Delete uses button-text-swap pattern: first click changes "Delete" to "Are you sure?" (red). Second click calls `onDelete`. Clicking outside or after 3s timeout resets.

**After delete:** remove the transaction from local state (optimistic removal). No full re-fetch needed.

**Edit:** navigates to `/transactions/:id/edit`

### TransactionForm edit mode

Extend existing `TransactionForm` to support editing.

**File:** `web/src/components/TransactionForm/TransactionForm.tsx`

- New route: `/transactions/:id/edit`
- Add `useParams()` import to read optional `:id` param
- When `id` is present: fetch transaction via `getTransaction(id)`, pre-fill all fields, set transaction type from the response
- Lock the type segmented button when editing (type is derived from the category)
- Title: "Edit Transaction", submit button: "Update Transaction"
- On submit: call `updateTransaction(id, body)` instead of `createTransaction(body)`
- After submit: `navigate(-1)` to return to the transaction list

### API additions

**File:** `web/src/api/budgetApi.ts`

- Add `budgetItemId?: number` to `getTransactions()` params

### Entry points

**BudgetCategoryRow** (`web/src/components/BudgetCategoryRow/BudgetCategoryRow.tsx`):
- Wrap row in a `<Link>` to `/budgets/{year}/{month}/items/{item.id}/transactions`
- Add `year` and `month` props (passed from `BudgetList`)
- Update `BudgetList.tsx` to pass `year` and `month` to each `BudgetCategoryRow`
- CSS: underline category name text on row hover to indicate clickability

**EditPlan income items** (`web/src/components/EditPlan/EditPlan.tsx`):
- Wrap each income item row in a `<Link>` to `/budgets/{year}/{month}/items/{item.id}/transactions`
- `item.id` here is `BudgetItemResponse.id` — the budget item ID, which is the correct value for the URL
- Stays read-only (no allocation editing)

### Route additions

**File:** `web/src/App.tsx`

- `/transactions/:id/edit` → `TransactionForm`
- `/budgets/:year/:month/items/:itemId/transactions` → `TransactionList`

## New files

- `web/src/components/TransactionList/TransactionList.tsx`
- `web/src/components/TransactionList/TransactionList.css`
- `web/src/components/TransactionList/TransactionRow.tsx`

## Verification

1. `./gradlew test` — backend tests pass
2. Navigate from BudgetList → click expense row → see transaction list with correct transactions
3. Navigate from EditPlan → click income row → see transaction list
4. Edit a transaction → form pre-filled, type locked → update succeeds → navigates back to list
5. Delete a transaction → first click shows "Are you sure?" → second click deletes → row removed from list
6. Empty state displays correctly when no transactions exist