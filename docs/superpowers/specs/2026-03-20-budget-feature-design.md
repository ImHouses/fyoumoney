# Budget Feature Design — fyoumoney Server

**Date:** 2026-03-20
**Status:** Approved

## Overview

The budget is the central feature of fyoumoney. Users define global categories (e.g., Food, Salary, Transport) with a default allocation. Each month automatically gets a budget populated from all active categories when first accessed. Transactions are linked to budget items, allowing the user to see how much they've spent vs. allocated per category per month.

## Data Model

### Categories

| Column | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| name | String | e.g., "Food", "Salary" |
| type | TransactionType | INCOME or EXPENSE |
| defaultAllocationCents | Long | Default monthly allocation in cents |
| active | Boolean | Default true. Set to false on delete. |

### Budgets

| Column | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| year | Int | |
| month | Int | |

Unique constraint on `(year, month)`.

### BudgetItems

| Column | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | |
| budgetId | Int (FK → Budgets) | |
| categoryId | Int (FK → Categories) | |
| allocationCents | Long | Copied from category default, then editable. Must be >= 0. |
| snoozed | Boolean | Default false |

Unique constraint on `(budgetId, categoryId)` — prevents duplicate items for the same category in a month.

### Transactions (modified)

Existing table changes:

| Column | Type | Notes |
|---|---|---|
| budgetItemId | Int (FK → BudgetItems) | NEW — links transaction to a specific category in a specific month's budget |

The `type` column remains in the Transactions table for query convenience, but is **set automatically from the category's type** when creating or updating a transaction. It is not sent by the frontend.

### Schema Cross-Reference Exception

`TransactionSchema.kt` references `BudgetItemsTable` from the budgets feature for the FK constraint. This is a deliberate, documented exception to the architecture spec's cross-feature access rule. The FK is necessary for data integrity. No other cross-schema references are permitted between features.

## Feature Structure

```
features/
├── categories/
│   ├── CategoryModels.kt
│   ├── CategorySchema.kt
│   ├── CategoryRepository.kt
│   ├── CategoryService.kt
│   ├── CategoryRoutes.kt
│   └── CategoryModule.kt
├── budgets/
│   ├── BudgetModels.kt
│   ├── BudgetSchema.kt        # Defines both Budgets and BudgetItems tables
│   ├── BudgetRepository.kt
│   ├── BudgetService.kt
│   ├── BudgetRoutes.kt
│   └── BudgetModule.kt
└── transactions/               # Already exists, modified
    ├── TransactionModels.kt    # Add budgetItemId, remove type from request
    ├── TransactionSchema.kt    # Add FK to BudgetItems
    ├── TransactionRepository.kt
    ├── TransactionService.kt   # Depends on BudgetService for auto-create
    ├── TransactionRoutes.kt
    └── TransactionModule.kt
```

### Dependency Chain

- **Categories** — standalone, no dependencies
- **Budgets** — depends on `CategoryService` (to get all active categories when auto-creating a budget)
- **Transactions** — depends on `BudgetService` (to find or auto-create the right budget item when linking)

Cross-feature access follows the architecture spec rule: features depend on each other's *service*, never on the repository or schema directly. The only exception is the FK in `TransactionSchema.kt` documented above.

## API Endpoints

### Categories

| Method | Path | Description |
|---|---|---|
| POST | `/categories` | Create a category |
| GET | `/categories` | List all active categories |
| GET | `/categories/{id}` | Get one category |
| PUT | `/categories/{id}` | Update name, type, or default allocation |
| DELETE | `/categories/{id}` | Soft delete — sets `active = false`. Stays in existing budget items but excluded from future budgets. |

### Budgets

| Method | Path | Description |
|---|---|---|
| GET | `/budgets` | List all existing budgets. Supports optional `?year=2026` filter. |
| GET | `/budgets/{year}/{month}` | Get budget for that month. Auto-creates with all active categories and default allocations if none exists. Returns budget items with spent totals. |
| PUT | `/budgets/{year}/{month}/items/{itemId}` | Update a budget item's allocation or snoozed flag |

### Transactions (modified)

| Method | Path | Description |
|---|---|---|
| POST | `/transactions` | Create a transaction. Request includes `categoryId` and `date` (no `type` — inferred from category). Backend resolves the correct budget item, auto-creating the budget if needed. |
| GET | `/transactions` | List all transactions. Supports optional `?categoryId=X&year=Y&month=M` filters. Response includes `budgetItemId` and `categoryId`. |
| GET | `/transactions/{id}` | Get one transaction. Response includes `budgetItemId` and `categoryId`. |
| PUT | `/transactions/{id}` | Update a transaction. If `categoryId` or `date` changes, `budgetItemId` is re-resolved (potentially auto-creating a new budget). `type` is re-inferred from the new category. |
| DELETE | `/transactions/{id}` | Delete a transaction (unchanged) |

## Key Behaviors

### Budget Auto-Creation

When a budget is accessed (via `GET /budgets/{year}/{month}` or when creating/updating a transaction for a month without a budget):

1. Check if a budget exists for that year/month
2. If not, create one
3. For each active category (`active = true`), create a `BudgetItem` with the category's `defaultAllocationCents` and `snoozed = false`
4. Return the budget with all items

**Atomicity:** Budget auto-creation writes to two tables (Budgets + BudgetItems) and must run inside a single `newSuspendedTransaction` per the architecture spec's multi-step transaction pattern. When triggered from transaction creation, `TransactionService` owns the outer `newSuspendedTransaction` that encompasses the entire operation (budget creation + transaction insertion).

### Transaction → Budget Item Resolution

When creating or updating a transaction, the frontend sends `categoryId` and `date`. The backend:

1. Extracts year/month from the transaction date
2. Finds or auto-creates the budget for that month
3. Finds the budget item matching the given `categoryId` in that budget
4. Sets the transaction's `type` from the category's `type`
5. Links the transaction to that budget item via `budgetItemId`

### Transactions with Soft-Deleted Categories

When creating a transaction referencing a soft-deleted category (`active = false`):
- **If a budget item already exists** for that category in the target month's budget: allow it. The user is logging against historical data.
- **If no budget item exists** (budget was created after the category was deleted): reject with 422. The category is no longer available for new budget items.

### Category Soft Delete

When a category is deleted:
- `active` is set to `false`
- Existing budget items referencing it remain (historical data preserved)
- Future auto-created budgets will not include it

### Budget Spent Totals

`GET /budgets/{year}/{month}` returns each budget item with a computed `spentCents` field — the sum of all transaction amounts linked to that budget item. This is calculated at query time, not stored.