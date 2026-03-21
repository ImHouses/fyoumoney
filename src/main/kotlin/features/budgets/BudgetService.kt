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

    suspend fun getOrCreateBudget(year: Int, month: Int): BudgetResponse {
        val budget = repository.findBudgetByYearMonth(year, month)
            ?: autoCreateBudget(year, month)

        val items = repository.findBudgetItemsByBudgetId(budget.id)
        val spentByItem = getSpentByBudgetItems(items.map { it.id })

        var totalExpenseCents = 0L
        var totalIncomeCents = 0L

        val itemResponses = items.map { item ->
            val category = categoryService.getById(item.categoryId)!!
            val itemSpentCents = spentByItem[item.id] ?: 0L

            when (category.type) {
                TransactionType.EXPENSE -> totalExpenseCents += itemSpentCents
                TransactionType.INCOME -> totalIncomeCents += itemSpentCents
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

        val remainingCents = totalExpenseCents - totalIncomeCents

        return BudgetResponse(
            id = budget.id,
            year = budget.year,
            month = budget.month,
            spent = BigDecimal(totalExpenseCents).movePointLeft(2).toPlainString(),
            remaining = BigDecimal(remainingCents).movePointLeft(2).toPlainString(),
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
        if (itemIds.isEmpty()) return emptyMap()
        return newSuspendedTransaction(Dispatchers.IO) {
            val sumCol = Transactions.amountCents.sum()
            Transactions.select(Transactions.budgetItemId, sumCol)
                .where { Transactions.budgetItemId inList itemIds }
                .groupBy(Transactions.budgetItemId)
                .associate { row ->
                    row[Transactions.budgetItemId] to (row[sumCol] ?: 0L)
                }
        }
    }
}
