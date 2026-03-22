# Setup Form Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-time onboarding form that lets users create income and expense categories before using the budget view.

**Architecture:** New batch endpoint on the backend (`POST /categories/batch`). New `/setup` route on the frontend with `SetupPage` and `CategoryRow` components. Root route checks if categories exist and redirects accordingly.

**Tech Stack:** Ktor/Exposed (backend batch endpoint), React + react-router-dom (frontend)

**Spec:** `docs/superpowers/specs/2026-03-21-setup-form-design.md`

---

### File Map

| File | Action | Responsibility |
|---|---|---|
| `src/main/kotlin/features/categories/CategoryRepository.kt` | Modify | Add `createBatch()` |
| `src/main/kotlin/features/categories/CategoryService.kt` | Modify | Add `createBatch()` |
| `src/main/kotlin/features/categories/CategoryRoutes.kt` | Modify | Add `POST /categories/batch` |
| `src/test/kotlin/features/categories/CategoryRepositoryTest.kt` | Modify | Add batch test |
| `src/test/kotlin/features/categories/CategoryRoutesTest.kt` | Modify | Add batch route test |
| `web/src/api/budgetApi.ts` | Modify | Add `createCategories()`, fix `createCategory` return type |
| `web/src/hooks/useCategories.ts` | Create | Hook to fetch categories (used by root redirect and setup guard) |
| `web/src/components/CategoryRow/CategoryRow.tsx` | Create | Single category display/edit component |
| `web/src/components/CategoryRow/CategoryRow.css` | Create | CategoryRow styles |
| `web/src/components/SetupPage/SetupPage.tsx` | Create | Onboarding page with category management |
| `web/src/components/SetupPage/SetupPage.css` | Create | SetupPage styles |
| `web/src/App.tsx` | Modify | Add `/setup` route, add category check on root |

---

### Task 1: Backend — Batch create in repository

**Files:**
- Modify: `src/main/kotlin/features/categories/CategoryRepository.kt`
- Modify: `src/test/kotlin/features/categories/CategoryRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `CategoryRepositoryTest.kt`:

```kotlin
@Test
fun `createBatch stores multiple categories and returns their ids`() = runBlocking {
    val ids = repository.createBatch(
        listOf(
            NewCategory("Salary", TransactionType.INCOME, 500000),
            NewCategory("Food", TransactionType.EXPENSE, 50000),
            NewCategory("Rent", TransactionType.EXPENSE, 120000),
        )
    )
    assertEquals(3, ids.size)
    ids.forEach { assertTrue(it > 0) }
    val all = repository.findAllActive()
    assertEquals(3, all.size)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRepositoryTest.createBatch stores multiple categories and returns their ids"`
Expected: FAIL — `createBatch` does not exist.

- [ ] **Step 3: Implement `createBatch` in `CategoryRepository.kt`**

Add this method to `CategoryRepository`. Uses Exposed's `batchInsert` for a single multi-row INSERT statement:

```kotlin
suspend fun createBatch(categories: List<NewCategory>): List<Int> = newSuspendedTransaction(Dispatchers.IO) {
    Categories.batchInsert(categories) { category ->
        this[Categories.name] = category.name
        this[Categories.type] = category.type
        this[Categories.defaultAllocationCents] = category.defaultAllocationCents
    }.map { it[Categories.id] }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRepositoryTest"`
Expected: All PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/categories/CategoryRepository.kt src/test/kotlin/features/categories/CategoryRepositoryTest.kt
git commit -m "feat: add batch create to CategoryRepository"
```

---

### Task 2: Backend — Batch create in service and routes

**Files:**
- Modify: `src/main/kotlin/features/categories/CategoryService.kt`
- Modify: `src/main/kotlin/features/categories/CategoryRoutes.kt`
- Modify: `src/test/kotlin/features/categories/CategoryRoutesTest.kt`

- [ ] **Step 1: Add `createBatch` to `CategoryService.kt`**

```kotlin
suspend fun createBatch(categories: List<NewCategory>): List<Int> = repository.createBatch(categories)
```

- [ ] **Step 2: Add batch route to `CategoryRoutes.kt`**

Add inside the `routing { }` block, before the existing `post("/categories")`:

```kotlin
post("/categories/batch") {
    val requests = call.receive<List<CategoryRequest>>()
    val ids = service.createBatch(requests.map { it.toNewCategory() })
    call.respond(HttpStatusCode.Created, ids)
}
```

- [ ] **Step 3: Write route test**

Add to `CategoryRoutesTest.kt`:

```kotlin
@Test
fun `POST categories batch creates multiple and returns 201 with ids`() = withApp {
    val response = client.post("/categories/batch") {
        contentType(ContentType.Application.Json)
        setBody("""[
            {"name":"Salary","type":"INCOME","defaultAllocation":"5000.00"},
            {"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"},
            {"name":"Rent","type":"EXPENSE","defaultAllocation":"1200.00"}
        ]""")
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val body = response.bodyAsText()
    // Response is a JSON array of 3 IDs
    assertContains(body, ",")

    // Verify all 3 categories exist
    val getResponse = client.get("/categories")
    val getBody = getResponse.bodyAsText()
    assertContains(getBody, "Salary")
    assertContains(getBody, "Food")
    assertContains(getBody, "Rent")
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests "dev.jcasas.features.categories.*"`
Expected: All PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/categories/CategoryService.kt src/main/kotlin/features/categories/CategoryRoutes.kt src/test/kotlin/features/categories/CategoryRoutesTest.kt
git commit -m "feat: add POST /categories/batch endpoint"
```

---

### Task 3: Frontend — API client updates

**Files:**
- Modify: `web/src/api/budgetApi.ts`

- [ ] **Step 1: Fix `createCategory` return type and add `createCategories`**

In `budgetApi.ts`, change:

```ts
export const createCategory = (body: CategoryRequest) =>
  request<CategoryResponse>('/categories', { method: 'POST', body: JSON.stringify(body) });
```

To:

```ts
export const createCategory = (body: CategoryRequest) =>
  request<number>('/categories', { method: 'POST', body: JSON.stringify(body) });

export const createCategories = (body: CategoryRequest[]) =>
  request<number[]>('/categories/batch', { method: 'POST', body: JSON.stringify(body) });
```

- [ ] **Step 2: Commit**

```bash
git add web/src/api/budgetApi.ts
git commit -m "feat: add createCategories batch API, fix createCategory return type"
```

---

### Task 4: Frontend — `useCategories` hook

**Files:**
- Create: `web/src/hooks/useCategories.ts`

- [ ] **Step 1: Create the hook**

```ts
import { useState, useEffect } from 'react';
import type { CategoryResponse } from '../types/budget';
import { getCategories } from '../api/budgetApi';

interface UseCategoriesResult {
  categories: CategoryResponse[];
  loading: boolean;
  error: string | null;
}

export function useCategories(): UseCategoriesResult {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load categories'))
      .finally(() => setLoading(false));
  }, []);

  return { categories, loading, error };
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/hooks/useCategories.ts
git commit -m "feat: add useCategories hook"
```

---

### Task 5: Frontend — `CategoryRow` component

**Files:**
- Create: `web/src/components/CategoryRow/CategoryRow.tsx`
- Create: `web/src/components/CategoryRow/CategoryRow.css`

- [ ] **Step 1: Create `CategoryRow.css`**

```css
.category-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background-color: var(--color-bg-card);
  border-radius: var(--radius-md);
  transition: background-color var(--transition-base);
}

.category-row-emoji {
  font-size: 20px;
  line-height: 1;
}

.category-row-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
}

.category-row-allocation {
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
}

.category-row-actions {
  display: flex;
  gap: 6px;
}

.category-row-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  font-size: 14px;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.category-row-btn:hover {
  background-color: var(--color-bg-card-hover);
  color: var(--color-text-primary);
}

.category-row-btn.delete:hover {
  color: var(--color-warning-text);
}

/* Edit mode */
.category-row-edit {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  background-color: var(--color-bg-card);
  border-radius: var(--radius-md);
  transition: background-color var(--transition-base);
}

.category-row-edit-fields {
  display: flex;
  gap: 10px;
}

.category-row-edit-fields input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background-color: transparent;
  color: var(--color-text-primary);
  font-family: var(--font-family);
  font-size: 14px;
  outline: none;
  transition: border-color var(--transition-fast);
}

.category-row-edit-fields input:focus {
  border-color: var(--color-text-secondary);
}

.category-row-edit-fields input::placeholder {
  color: var(--color-text-muted);
}

.category-row-edit-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.category-row-edit-btn {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-sm);
  font-family: var(--font-family);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.category-row-edit-btn:hover {
  opacity: 0.85;
}

.category-row-edit-btn.save {
  background-color: var(--color-btn-primary-bg);
  color: var(--color-btn-primary-text);
}

.category-row-edit-btn.cancel {
  background: transparent;
  color: var(--color-text-muted);
}
```

- [ ] **Step 2: Create `CategoryRow.tsx`**

Uses the same `formatAmountDisplay` and `toRawAmount` helpers as TransactionForm. These should be extracted to `utils/format.ts` first.

Add to `web/src/utils/format.ts`:

```ts
export function formatAmountDisplay(value: string): string {
  const digits = value.replace(/[^0-9]/g, '');
  if (!digits) return '';
  const cents = digits.padStart(3, '0');
  const dollars = cents.slice(0, -2);
  const decimal = cents.slice(-2);
  const trimmedDollars = dollars.replace(/^0+/, '') || '0';
  const withCommas = trimmedDollars.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `$${withCommas}.${decimal}`;
}

export function toRawAmount(value: string): string {
  const digits = value.replace(/[^0-9]/g, '');
  if (!digits) return '';
  const cents = digits.padStart(3, '0');
  const dollars = cents.slice(0, -2);
  const decimal = cents.slice(-2);
  return `${parseInt(dollars, 10) || 0}.${decimal}`;
}
```

Then remove the duplicate definitions from `TransactionForm.tsx` and import from `utils/format.ts`.

Now create `CategoryRow.tsx`:

```tsx
import { useState, useCallback } from 'react';
import type { TransactionType } from '../../types/budget';
import { formatCurrency, getCategoryEmoji, formatAmountDisplay, toRawAmount } from '../../utils/format';
import './CategoryRow.css';

export interface LocalCategory {
  tempId: string;
  name: string;
  type: TransactionType;
  defaultAllocation: string;
  editing: boolean;
  isNew: boolean;
}

interface CategoryRowProps {
  category: LocalCategory;
  onSave: (updated: LocalCategory) => void;
  onDelete: (tempId: string) => void;
  onCancel: (tempId: string) => void;
}

export function CategoryRow({ category, onSave, onDelete, onCancel }: CategoryRowProps) {
  const [name, setName] = useState(category.name);
  const [displayAllocation, setDisplayAllocation] = useState(
    category.defaultAllocation ? formatAmountDisplay(category.defaultAllocation.replace('.', '')) : '',
  );
  const [rawAllocation, setRawAllocation] = useState(category.defaultAllocation);

  const handleAllocationChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target.value;
    setDisplayAllocation(formatAmountDisplay(input));
    setRawAllocation(toRawAmount(input));
  }, []);

  const handleSave = () => {
    if (!name.trim() || !rawAllocation) return;
    onSave({
      ...category,
      name: name.trim(),
      defaultAllocation: rawAllocation,
      editing: false,
      isNew: false,
    });
  };

  const handleCancel = () => {
    onCancel(category.tempId);
  };

  if (category.editing) {
    return (
      <div className="category-row-edit">
        <div className="category-row-edit-fields">
          <input
            type="text"
            placeholder="Category name"
            value={name}
            onChange={e => setName(e.target.value)}
            autoFocus
          />
          <input
            type="text"
            inputMode="numeric"
            placeholder="$0.00"
            value={displayAllocation}
            onChange={handleAllocationChange}
          />
        </div>
        <div className="category-row-edit-actions">
          <button className="category-row-edit-btn cancel" onClick={handleCancel}>Cancel</button>
          <button className="category-row-edit-btn save" onClick={handleSave}>Save</button>
        </div>
      </div>
    );
  }

  return (
    <div className="category-row">
      <span className="category-row-emoji">{getCategoryEmoji(category.name)}</span>
      <span className="category-row-name">{category.name}</span>
      <span className="category-row-allocation">{formatCurrency(category.defaultAllocation)}</span>
      <div className="category-row-actions">
        <button
          className="category-row-btn"
          onClick={() => onSave({ ...category, editing: true })}
          aria-label="Edit"
        >
          ✏️
        </button>
        <button
          className="category-row-btn delete"
          onClick={() => onDelete(category.tempId)}
          aria-label="Delete"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Update TransactionForm.tsx**

Remove the `formatAmountDisplay` and `toRawAmount` function definitions from `TransactionForm.tsx` and import them from `utils/format.ts`:

```ts
import { formatAmountDisplay, toRawAmount } from '../../utils/format';
```

- [ ] **Step 4: Commit**

```bash
git add web/src/utils/format.ts web/src/components/CategoryRow/ web/src/components/TransactionForm/TransactionForm.tsx
git commit -m "feat: add CategoryRow component, extract format helpers"
```

---

### Task 6: Frontend — `SetupPage` component

**Files:**
- Create: `web/src/components/SetupPage/SetupPage.tsx`
- Create: `web/src/components/SetupPage/SetupPage.css`

- [ ] **Step 1: Create `SetupPage.css`**

```css
.setup-page {
  max-width: 520px;
  margin: 0 auto;
  padding: 40px 20px;
}

.setup-header {
  margin-bottom: 36px;
}

.setup-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
}

.setup-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.setup-section {
  margin-bottom: 28px;
}

.setup-section-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.setup-section-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.setup-add-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-family: var(--font-family);
  font-size: 14px;
  cursor: pointer;
  transition: border-color var(--transition-fast), color var(--transition-fast);
}

.setup-add-btn:hover {
  border-color: var(--color-text-secondary);
  color: var(--color-text-primary);
}

.setup-summary {
  padding: 16px;
  border-radius: var(--radius-md);
  background-color: var(--color-summary-bg);
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  transition: background-color var(--transition-base);
}

.setup-summary-row {
  display: flex;
  justify-content: space-between;
}

.setup-summary-label {
  color: var(--color-text-secondary);
}

.setup-summary-value {
  font-weight: 600;
}

.setup-summary-value.remaining {
  color: var(--color-summary-remaining-text);
}

.setup-done-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: var(--radius-sm);
  background-color: var(--color-btn-primary-bg);
  color: var(--color-btn-primary-text);
  font-family: var(--font-family);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity var(--transition-fast), background-color var(--transition-base);
}

.setup-done-btn:hover {
  opacity: 0.9;
}

.setup-done-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.setup-error {
  color: var(--color-warning-text);
  font-size: 14px;
  text-align: center;
  margin-bottom: 12px;
}
```

- [ ] **Step 2: Create `SetupPage.tsx`**

```tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategories, createCategories } from '../../api/budgetApi';
import type { CategoryRequest } from '../../types/budget';
import { formatCurrency, parseDecimal } from '../../utils/format';
import { CategoryRow, type LocalCategory } from '../CategoryRow/CategoryRow';
import './SetupPage.css';

export function SetupPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<LocalCategory[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guardLoading, setGuardLoading] = useState(true);

  // Guard: redirect if categories already exist
  useEffect(() => {
    getCategories()
      .then(existing => {
        if (existing.length > 0) navigate('/', { replace: true });
        else setGuardLoading(false);
      })
      .catch(() => setGuardLoading(false));
  }, [navigate]);

  const incomeCategories = categories.filter(c => c.type === 'INCOME');
  const expenseCategories = categories.filter(c => c.type === 'EXPENSE');

  const totalIncome = incomeCategories
    .filter(c => !c.editing)
    .reduce((sum, c) => sum + parseDecimal(c.defaultAllocation), 0);
  const totalExpenses = expenseCategories
    .filter(c => !c.editing)
    .reduce((sum, c) => sum + parseDecimal(c.defaultAllocation), 0);
  const remaining = totalIncome - totalExpenses;

  const addCategory = (type: 'INCOME' | 'EXPENSE') => {
    setCategories(prev => [
      ...prev,
      {
        tempId: crypto.randomUUID(),
        name: '',
        type,
        defaultAllocation: '',
        editing: true,
        isNew: true,
      },
    ]);
  };

  const handleSave = (updated: LocalCategory) => {
    setCategories(prev => prev.map(c => (c.tempId === updated.tempId ? updated : c)));
  };

  const handleDelete = (tempId: string) => {
    setCategories(prev => prev.filter(c => c.tempId !== tempId));
  };

  const handleCancel = (tempId: string) => {
    setCategories(prev => {
      const cat = prev.find(c => c.tempId === tempId);
      if (cat?.isNew) return prev.filter(c => c.tempId !== tempId);
      return prev.map(c => (c.tempId === tempId ? { ...c, editing: false } : c));
    });
  };

  const hasIncome = incomeCategories.some(c => !c.editing && c.name);
  const hasExpense = expenseCategories.some(c => !c.editing && c.name);
  const canSubmit = hasIncome && hasExpense && !submitting;

  const handleDone = async () => {
    setError(null);
    setSubmitting(true);

    const saved = categories.filter(c => !c.editing && c.name);
    const requests: CategoryRequest[] = saved.map(c => ({
      name: c.name,
      type: c.type,
      defaultAllocation: c.defaultAllocation,
    }));

    try {
      await createCategories(requests);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save categories');
    } finally {
      setSubmitting(false);
    }
  };

  if (guardLoading) return null;

  return (
    <div className="setup-page">
      <div className="setup-header">
        <h1 className="setup-title">Before anything...</h1>
        <p className="setup-subtitle">
          Set up your budget plan for a typical month. Don't worry — you can customize your spending anytime!
        </p>
      </div>

      <div className="setup-section">
        <div className="setup-section-label">Income</div>
        <div className="setup-section-items">
          {incomeCategories.map(cat => (
            <CategoryRow
              key={cat.tempId}
              category={cat}
              onSave={handleSave}
              onDelete={handleDelete}
              onCancel={handleCancel}
            />
          ))}
        </div>
        <button className="setup-add-btn" onClick={() => addCategory('INCOME')}>
          + Add income source
        </button>
      </div>

      <div className="setup-section">
        <div className="setup-section-label">Expenses</div>
        <div className="setup-section-items">
          {expenseCategories.map(cat => (
            <CategoryRow
              key={cat.tempId}
              category={cat}
              onSave={handleSave}
              onDelete={handleDelete}
              onCancel={handleCancel}
            />
          ))}
        </div>
        <button className="setup-add-btn" onClick={() => addCategory('EXPENSE')}>
          + Add spending category
        </button>
      </div>

      {(hasIncome || hasExpense) && (
        <div className="setup-summary">
          <div className="setup-summary-row">
            <span className="setup-summary-label">Monthly income</span>
            <span className="setup-summary-value">{formatCurrency(String(totalIncome))}</span>
          </div>
          <div className="setup-summary-row">
            <span className="setup-summary-label">Monthly expenses</span>
            <span className="setup-summary-value">{formatCurrency(String(totalExpenses))}</span>
          </div>
          <div className="setup-summary-row">
            <span className="setup-summary-label">Remaining</span>
            <span className="setup-summary-value remaining">{formatCurrency(String(remaining))}</span>
          </div>
        </div>
      )}

      {error && <div className="setup-error">{error}</div>}

      <button className="setup-done-btn" disabled={!canSubmit} onClick={handleDone}>
        {submitting ? 'Saving...' : "I'm done, let's go!"}
      </button>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add web/src/components/SetupPage/
git commit -m "feat: add SetupPage onboarding component"
```

---

### Task 7: Frontend — Wire up routing with category check

**Files:**
- Modify: `web/src/App.tsx`

- [ ] **Step 1: Update `App.tsx`**

Replace the current `App` function and add the `RootRedirect` component. Keep `BudgetPage` as-is.

```tsx
import { useState, useEffect } from 'react';
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
  // ... existing BudgetPage code unchanged ...
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
```

Note: Keep the full `BudgetPage` function exactly as it is today — only change the `App` function and add `RootRedirect`.

- [ ] **Step 2: Commit**

```bash
git add web/src/App.tsx
git commit -m "feat: add /setup route with category-based redirect"
```

---

### Verification

After all tasks are complete:

1. Reset database: `docker compose down -v && docker compose up -d --wait`
2. Start backend: `./gradlew run`
3. Open `http://localhost:5173` — should redirect to `/setup`
4. Add income and expense categories, click "Done"
5. Should redirect to `/` with budget view populated from the new categories
6. Refresh page — should stay on `/` (not redirect back to setup)
