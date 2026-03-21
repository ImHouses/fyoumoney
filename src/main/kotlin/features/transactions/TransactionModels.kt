package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val id: Int,
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
    val budgetItemId: Int,
    val categoryId: Int,
)

data class NewTransaction(
    val amount: BigDecimal,
    val categoryId: Int,
    val description: String,
    val date: LocalDate,
)

@Serializable
data class TransactionRequest(
    val amount: String,
    val categoryId: Int,
    val description: String,
    val date: String,
) {
    fun toNewTransaction() = NewTransaction(
        amount = BigDecimal(amount),
        categoryId = categoryId,
        description = description,
        date = LocalDate.parse(date),
    )
}

@Serializable
data class TransactionResponse(
    val id: Int,
    val amount: String,
    val type: TransactionType,
    val description: String,
    val date: String,
    val budgetItemId: Int,
    val categoryId: Int,
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            id = transaction.id,
            amount = transaction.amount.toPlainString(),
            type = transaction.type,
            description = transaction.description,
            date = transaction.date.toString(),
            budgetItemId = transaction.budgetItemId,
            categoryId = transaction.categoryId,
        )
    }
}
