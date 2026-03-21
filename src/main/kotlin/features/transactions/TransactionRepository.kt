package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class TransactionRepository {

    suspend fun create(transaction: NewTransaction, budgetItemId: Int, type: TransactionType): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.insert {
                it[amountCents] = transaction.amount.movePointRight(2).toLong()
                it[Transactions.type] = type
                it[description] = transaction.description
                it[date] = transaction.date
                it[Transactions.budgetItemId] = budgetItemId
            }[Transactions.id]
        }

    suspend fun findById(id: Int): Transaction? = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.join(BudgetItems, JoinType.INNER, Transactions.budgetItemId, BudgetItems.id)
            .selectAll()
            .where { Transactions.id eq id }
            .map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                    budgetItemId = row[Transactions.budgetItemId],
                    categoryId = row[BudgetItems.categoryId],
                )
            }
            .singleOrNull()
    }

    suspend fun findAll(categoryId: Int? = null, year: Int? = null, month: Int? = null): List<Transaction> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = Transactions
                .join(BudgetItems, JoinType.INNER, Transactions.budgetItemId, BudgetItems.id)
                .join(Budgets, JoinType.INNER, BudgetItems.budgetId, Budgets.id)
                .selectAll()

            if (categoryId != null) {
                query.andWhere { BudgetItems.categoryId eq categoryId }
            }
            if (year != null) {
                query.andWhere { Budgets.year eq year }
            }
            if (month != null) {
                query.andWhere { Budgets.month eq month }
            }

            query.map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                    budgetItemId = row[Transactions.budgetItemId],
                    categoryId = row[BudgetItems.categoryId],
                )
            }
        }

    suspend fun update(id: Int, transaction: NewTransaction, budgetItemId: Int, type: TransactionType) =
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.update({ Transactions.id eq id }) {
                it[amountCents] = transaction.amount.movePointRight(2).toLong()
                it[Transactions.type] = type
                it[description] = transaction.description
                it[date] = transaction.date
                it[Transactions.budgetItemId] = budgetItemId
            }
        }

    suspend fun delete(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.deleteWhere { Transactions.id eq id }
    }
}
