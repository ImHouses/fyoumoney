package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.transactions.TransactionRepository
import java.math.BigDecimal

class BudgetService(
    private val repository: BudgetRepository,
    private val categoryService: CategoryService,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun getOrCreateBudget(
        year: Int,
        month: Int,
    ): BudgetResponse {
        val activeCategories = categoryService.getAllActive()
        val categoryMap = activeCategories.associateBy { it.id }

        val budget =
            repository.findBudgetByYearMonth(year, month)
                ?: autoCreateBudget(year, month, activeCategories.map { it.id to it.defaultAllocationCents })

        var items = repository.findBudgetItemsByBudgetId(budget.id)

        // Add budget items for any active categories missing from this budget
        val existingCategoryIds = items.map { it.categoryId }.toSet()
        val missing = activeCategories.filter { it.id !in existingCategoryIds }
        if (missing.isNotEmpty()) {
            val newItems =
                missing.map { cat ->
                    repository.createBudgetItem(budget.id, cat.id, cat.defaultAllocationCents)
                }
            items = items + newItems
        }

        val spentByItem = transactionRepository.getSpentByBudgetItems(items.map { it.id })

        var totalExpenseSpentCents = 0L
        var totalIncomeSpentCents = 0L

        val itemResponses =
            items.mapNotNull { item ->
                val category = categoryMap[item.categoryId] ?: return@mapNotNull null
                val itemSpentCents = spentByItem[item.id] ?: 0L

                when (category.type) {
                    TransactionType.EXPENSE -> if (!item.snoozed) totalExpenseSpentCents += itemSpentCents
                    TransactionType.INCOME -> totalIncomeSpentCents += itemSpentCents
                }

                BudgetItemResponse(
                    id = item.id,
                    categoryId = item.categoryId,
                    categoryName = category.name,
                    categoryType = category.type,
                    allocation = BigDecimal(item.allocationCents).movePointLeft(2).toPlainString(),
                    spent = BigDecimal(itemSpentCents).movePointLeft(2).toPlainString(),
                    snoozed = item.snoozed,
                )
            }

        val remainingCents = totalIncomeSpentCents - totalExpenseSpentCents

        return BudgetResponse(
            id = budget.id,
            year = budget.year,
            month = budget.month,
            spent = BigDecimal(totalExpenseSpentCents).movePointLeft(2).toPlainString(),
            remaining = BigDecimal(remainingCents).movePointLeft(2).toPlainString(),
            items = itemResponses,
        )
    }

    private suspend fun autoCreateBudget(
        year: Int,
        month: Int,
        categoryItems: List<Pair<Int, Long>>,
    ): Budget = repository.createBudgetWithItems(year, month, categoryItems)

    suspend fun findBudgetItemForTransaction(
        categoryId: Int,
        year: Int,
        month: Int,
    ): BudgetItem? {
        val budget =
            repository.findBudgetByYearMonth(year, month)
                ?: run {
                    val activeCategories = categoryService.getAllActive()
                    autoCreateBudget(year, month, activeCategories.map { it.id to it.defaultAllocationCents })
                }

        return repository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)
            ?: run {
                val category = categoryService.getById(categoryId) ?: return null
                if (!category.active) return null
                repository.createBudgetItem(budget.id, categoryId, category.defaultAllocationCents)
            }
    }

    suspend fun updateBudgetItem(
        itemId: Int,
        allocationCents: Long?,
        snoozed: Boolean?,
    ) {
        repository.updateBudgetItem(itemId, allocationCents, snoozed)
    }

    suspend fun getAllBudgets(year: Int?): List<BudgetSummaryResponse> =
        repository.findAllBudgets(year).map { budget ->
            BudgetSummaryResponse(
                id = budget.id,
                year = budget.year,
                month = budget.month,
            )
        }
}
