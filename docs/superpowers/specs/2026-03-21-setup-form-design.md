# Setup Form (First-Time Onboarding)

## Context

When the app loads with no categories, the budget view is empty and useless — no items, no transactions possible. We need a one-time setup form that collects the user's income sources and expense categories before they can use the app.

## Detection

`GET /categories` returns an empty list → redirect to `/setup`. Once categories exist, go straight to the budget view.

**Guard:** If a user navigates directly to `/setup` and categories already exist, redirect to `/`.

## Route

`/setup` — new route in `App.tsx`. After setup completes, navigate to `/`.

## Page Layout

```
SetupPage
├── Title: "Before anything..."
├── Subtitle: "Set up your budget plan for a typical month.
│             Don't worry — you can customize your spending anytime!"
├── Income Section
│   ├── "INCOME" label
│   ├── CategoryRow[] (type: INCOME)
│   └── "+ Add income source" button
├── Expense Section
│   ├── "EXPENSES" label
│   ├── CategoryRow[] (type: EXPENSE)
│   └── "+ Add spending category" button
├── Summary text
│   ├── "Monthly income: $X"
│   ├── "Monthly expenses: $Y"
│   └── "Remaining: $Z"
└── "I'm done, let's go!" button
```

## Components

### SetupPage (`components/SetupPage/SetupPage.tsx` + `.css`)

**State:** `categories: LocalCategory[]` — all categories live in local state until the user clicks Done.

```ts
interface LocalCategory {
  tempId: string;            // crypto.randomUUID() for React keys
  name: string;
  type: TransactionType;     // INCOME | EXPENSE
  defaultAllocation: string; // decimal string, e.g. "5000.00"
  editing: boolean;
  isNew: boolean;            // true until first save — used by cancel behavior
}
```

**Behavior:**
- "Add" buttons push a new empty `LocalCategory` with `editing: true` and `isNew: true`
- Summary is computed from local state (sum income allocations, sum expense allocations, difference)
- "Done" button:
  1. Validates: at least one income and one expense category required
  2. Calls `POST /categories/batch` with all categories at once
  3. On success, navigates to `/`
  4. On error, shows error message, does not navigate
- "Done" button disabled while no categories exist or while submitting
- On mount, fetches categories — if non-empty, redirects to `/` (setup guard)

### CategoryRow (`components/CategoryRow/CategoryRow.tsx` + `.css`)

Single component with two modes controlled by `editing` prop/state.

**Display mode:**
- Left: emoji (from `getCategoryEmoji` in `utils/format.ts`) + name
- Right: formatted allocation (e.g. "$5,000") + edit button + delete button
- Click edit → switches to edit mode

**Edit mode:**
- Name input (text)
- Default allocation input (formatted currency, same pattern as TransactionForm)
- Save button + Cancel button
- Save calls `onSave(updatedCategory)` — parent updates local state, sets `isNew: false`
- Cancel calls `onCancel()` — if `isNew`, removes from list; otherwise reverts to previous values

**Props:**
```ts
interface CategoryRowProps {
  category: LocalCategory;
  onSave: (updated: LocalCategory) => void;
  onDelete: (tempId: string) => void;
  onCancel: (tempId: string) => void;
}
```

## App.tsx Changes

The root `/` route checks if categories exist before rendering BudgetPage:

```tsx
function RootRedirect() {
  // Fetch categories, if empty redirect to /setup, otherwise render BudgetPage
  // Show loading state while fetching
}

<Routes>
  <Route path="/" element={<RootRedirect />} />
  <Route path="/setup" element={<SetupPage />} />
  <Route path="/transactions/new" element={<TransactionForm />} />
</Routes>
```

## Backend: Batch Categories Endpoint

### New route: `POST /categories/batch`

**Request:** `List<CategoryRequest>`

```json
[
  { "name": "Salary", "type": "INCOME", "defaultAllocation": "5000.00" },
  { "name": "Groceries", "type": "EXPENSE", "defaultAllocation": "400.00" },
  { "name": "Rent", "type": "EXPENSE", "defaultAllocation": "1200.00" }
]
```

**Response:** `List<Int>` (created IDs in order) — status 201

**Behavior:**
- Creates all categories in a single database transaction
- If any category fails, the entire batch rolls back — no partial state
- Consistent with existing `POST /categories` which returns an ID

### Files to modify (backend):
- `CategoryRoutes.kt` — add `post("/categories/batch")` route
- `CategoryService.kt` — add `createBatch(categories: List<NewCategory>): List<Int>`
- `CategoryRepository.kt` — add `insertBatch(categories: List<NewCategory>): List<Int>`

## Frontend API

### New function in `budgetApi.ts`:

```ts
export const createCategories = (body: CategoryRequest[]) =>
  request<number[]>('/categories/batch', { method: 'POST', body: JSON.stringify(body) });
```

### Fix existing type:

`createCategory` return type should be `number` not `CategoryResponse` (backend returns just the ID).

## Files Summary

| File | Action |
|---|---|
| `web/src/components/SetupPage/SetupPage.tsx` | Create |
| `web/src/components/SetupPage/SetupPage.css` | Create |
| `web/src/components/CategoryRow/CategoryRow.tsx` | Create |
| `web/src/components/CategoryRow/CategoryRow.css` | Create |
| `web/src/App.tsx` | Modify — add /setup route, category check on root with loading state |
| `web/src/api/budgetApi.ts` | Modify — add `createCategories()`, fix `createCategory` return type |
| `src/main/kotlin/features/categories/CategoryRoutes.kt` | Modify — add batch route |
| `src/main/kotlin/features/categories/CategoryService.kt` | Modify — add `createBatch()` |
| `src/main/kotlin/features/categories/CategoryRepository.kt` | Modify — add `insertBatch()` |
| `src/main/resources/openapi/documentation.yaml` | Modify — document batch endpoint |

## Visual Style

- Same dark/light theme as the rest of the app (CSS custom properties)
- Centered layout, max-width ~520px (similar to TransactionForm)
- Section labels use the same uppercase muted style as sidebar labels
- CategoryRow cards use `--color-bg-card` with `--radius-md`
- Buttons follow existing styling patterns (primary for Done, outlined for Add)