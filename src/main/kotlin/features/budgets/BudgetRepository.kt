package dev.jcasas.features.budgets

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class BudgetRepository {

    suspend fun findBudgetByYearMonth(year: Int, month: Int): Budget? = newSuspendedTransaction(Dispatchers.IO) {
        Budgets.selectAll()
            .where { (Budgets.year eq year) and (Budgets.month eq month) }
            .map { row ->
                Budget(
                    id = row[Budgets.id],
                    year = row[Budgets.year],
                    month = row[Budgets.month],
                )
            }
            .singleOrNull()
    }

    suspend fun findAllBudgets(year: Int?): List<Budget> = newSuspendedTransaction(Dispatchers.IO) {
        val query = Budgets.selectAll()
        if (year != null) {
            query.where { Budgets.year eq year }
        }
        query.map { row ->
            Budget(
                id = row[Budgets.id],
                year = row[Budgets.year],
                month = row[Budgets.month],
            )
        }
    }

    suspend fun createBudgetWithItems(
        year: Int,
        month: Int,
        items: List<Pair<Int, Long>>,
    ): Budget = newSuspendedTransaction(Dispatchers.IO) {
        val budgetId = Budgets.insert {
            it[Budgets.year] = year
            it[Budgets.month] = month
        }[Budgets.id]

        for ((categoryId, allocationCents) in items) {
            BudgetItems.insert {
                it[BudgetItems.budgetId] = budgetId
                it[BudgetItems.categoryId] = categoryId
                it[BudgetItems.allocationCents] = allocationCents
            }
        }

        Budget(id = budgetId, year = year, month = month)
    }

    suspend fun findBudgetItemById(id: Int): BudgetItem? = newSuspendedTransaction(Dispatchers.IO) {
        BudgetItems.selectAll()
            .where { BudgetItems.id eq id }
            .map { row ->
                BudgetItem(
                    id = row[BudgetItems.id],
                    budgetId = row[BudgetItems.budgetId],
                    categoryId = row[BudgetItems.categoryId],
                    allocationCents = row[BudgetItems.allocationCents],
                    snoozed = row[BudgetItems.snoozed],
                )
            }
            .singleOrNull()
    }

    suspend fun findBudgetItemByBudgetAndCategory(budgetId: Int, categoryId: Int): BudgetItem? =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.selectAll()
                .where { (BudgetItems.budgetId eq budgetId) and (BudgetItems.categoryId eq categoryId) }
                .map { row ->
                    BudgetItem(
                        id = row[BudgetItems.id],
                        budgetId = row[BudgetItems.budgetId],
                        categoryId = row[BudgetItems.categoryId],
                        allocationCents = row[BudgetItems.allocationCents],
                        snoozed = row[BudgetItems.snoozed],
                    )
                }
                .singleOrNull()
        }

    suspend fun findBudgetItemsByBudgetId(budgetId: Int): List<BudgetItem> =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.selectAll()
                .where { BudgetItems.budgetId eq budgetId }
                .map { row ->
                    BudgetItem(
                        id = row[BudgetItems.id],
                        budgetId = row[BudgetItems.budgetId],
                        categoryId = row[BudgetItems.categoryId],
                        allocationCents = row[BudgetItems.allocationCents],
                        snoozed = row[BudgetItems.snoozed],
                    )
                }
        }

    suspend fun createBudgetItem(budgetId: Int, categoryId: Int, allocationCents: Long): BudgetItem =
        newSuspendedTransaction(Dispatchers.IO) {
            val id = BudgetItems.insert {
                it[BudgetItems.budgetId] = budgetId
                it[BudgetItems.categoryId] = categoryId
                it[BudgetItems.allocationCents] = allocationCents
            }[BudgetItems.id]

            BudgetItem(id = id, budgetId = budgetId, categoryId = categoryId, allocationCents = allocationCents, snoozed = false)
        }

    suspend fun updateBudgetItem(id: Int, allocationCents: Long?, snoozed: Boolean?) =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.update({ BudgetItems.id eq id }) {
                if (allocationCents != null) it[BudgetItems.allocationCents] = allocationCents
                if (snoozed != null) it[BudgetItems.snoozed] = snoozed
            }
        }
}
