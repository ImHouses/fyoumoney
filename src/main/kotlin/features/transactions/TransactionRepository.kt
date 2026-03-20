package dev.jcasas.features.transactions

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class TransactionRepository {

    suspend fun create(transaction: NewTransaction): Int = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.insert {
            it[amountCents] = transaction.amount.movePointRight(2).toLong()
            it[type] = transaction.type
            it[description] = transaction.description
            it[date] = transaction.date
        }[Transactions.id]
    }

    suspend fun findById(id: Int): Transaction? = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.selectAll()
            .where { Transactions.id eq id }
            .map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                )
            }
            .singleOrNull()
    }

    suspend fun findAll(): List<Transaction> = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.selectAll()
            .map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                )
            }
    }

    suspend fun update(id: Int, transaction: NewTransaction) = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.update({ Transactions.id eq id }) {
            it[amountCents] = transaction.amount.movePointRight(2).toLong()
            it[type] = transaction.type
            it[description] = transaction.description
            it[date] = transaction.date
        }
    }

    suspend fun delete(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.deleteWhere { Transactions.id eq id }
    }
}