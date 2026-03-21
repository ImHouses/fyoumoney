package dev.jcasas.features.budgets

import dev.jcasas.features.categories.CategoryService

class BudgetService(
    private val repository: BudgetRepository,
    private val categoryService: CategoryService,
) {

    suspend fun getOrCreateBudget(year: Int, month: Int): BudgetResponse {
        val budget = repository.findBudgetByYearMonth(year, month)
            ?: autoCreateBudget(year, month)

        val items = repository.findBudgetItemsByBudgetId(budget.id)
        val spentByItem = getSpentByBudgetItems(items.map { it.id })

        val itemResponses = items.map { item ->
            val category = categoryService.getById(item.categoryId)!!
            BudgetItemResponse(
                id = item.id,
                categoryId = item.categoryId,
                categoryName = category.name,
                categoryType = category.type,
                allocationCents = item.allocationCents,
                spentCents = spentByItem[item.id] ?: 0L,
                snoozed = item.snoozed,
            )
        }

        return BudgetResponse(
            id = budget.id,
            year = budget.year,
            month = budget.month,
            items = itemResponses,
        )
    }

    private suspend fun autoCreateBudget(year: Int, month: Int): Budget {
        val activeCategories = categoryService.getAllActive()
        val items = activeCategories.map { it.id to it.defaultAllocationCents }
        return repository.createBudgetWithItems(year, month, items)
    }

    suspend fun findBudgetItemForTransaction(categoryId: Int, year: Int, month: Int): BudgetItem? {
        val budget = repository.findBudgetByYearMonth(year, month)
            ?: autoCreateBudget(year, month)

        return repository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)
    }

    suspend fun updateBudgetItem(itemId: Int, allocationCents: Long?, snoozed: Boolean?) {
        repository.updateBudgetItem(itemId, allocationCents, snoozed)
    }

    suspend fun getAllBudgets(year: Int?): List<BudgetSummaryResponse> {
        return repository.findAllBudgets(year).map { budget ->
            BudgetSummaryResponse(
                id = budget.id,
                year = budget.year,
                month = budget.month,
            )
        }
    }

    private suspend fun getSpentByBudgetItems(itemIds: List<Int>): Map<Int, Long> {
        // Placeholder: returns empty until Transactions schema adds budgetItemId (Task 13)
        return emptyMap()
    }
}
