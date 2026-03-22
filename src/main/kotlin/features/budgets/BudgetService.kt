package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.transactions.Transactions
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal

class BudgetService(
    private val repository: BudgetRepository,
    private val categoryService: CategoryService,
) {
    suspend fun getOrCreateBudget(
        year: Int,
        month: Int,
    ): BudgetResponse {
        val budget =
            repository.findBudgetByYearMonth(year, month)
                ?: autoCreateBudget(year, month)

        var items = repository.findBudgetItemsByBudgetId(budget.id)

        // Add budget items for any active categories missing from this budget
        val existingCategoryIds = items.map { it.categoryId }.toSet()
        val activeCategories = categoryService.getAllActive()
        val missing = activeCategories.filter { it.id !in existingCategoryIds }
        if (missing.isNotEmpty()) {
            val newItems =
                missing.map { cat ->
                    repository.createBudgetItem(budget.id, cat.id, cat.defaultAllocationCents)
                }
            items = items + newItems
        }

        val spentByItem = getSpentByBudgetItems(items.map { it.id })

        var totalExpenseSpentCents = 0L
        var totalIncomeAllocationCents = 0L

        val itemResponses =
            items.map { item ->
                val category = categoryService.getById(item.categoryId)!!
                val itemSpentCents = spentByItem[item.id] ?: 0L

                when (category.type) {
                    TransactionType.EXPENSE -> totalExpenseSpentCents += itemSpentCents
                    TransactionType.INCOME -> totalIncomeAllocationCents += item.allocationCents
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

        val remainingCents = totalIncomeAllocationCents - totalExpenseSpentCents

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
    ): Budget {
        val activeCategories = categoryService.getAllActive()
        val items = activeCategories.map { it.id to it.defaultAllocationCents }
        return repository.createBudgetWithItems(year, month, items)
    }

    suspend fun findBudgetItemForTransaction(
        categoryId: Int,
        year: Int,
        month: Int,
    ): BudgetItem? {
        val budget =
            repository.findBudgetByYearMonth(year, month)
                ?: autoCreateBudget(year, month)

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

    private suspend fun getSpentByBudgetItems(itemIds: List<Int>): Map<Int, Long> {
        if (itemIds.isEmpty()) return emptyMap()
        return newSuspendedTransaction(Dispatchers.IO) {
            val sumCol = Transactions.amountCents.sum()
            Transactions
                .select(Transactions.budgetItemId, sumCol)
                .where { Transactions.budgetItemId inList itemIds }
                .groupBy(Transactions.budgetItemId)
                .associate { row ->
                    row[Transactions.budgetItemId] to (row[sumCol] ?: 0L)
                }
        }
    }
}
