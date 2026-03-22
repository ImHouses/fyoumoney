package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Transactions : Table() {
    val id = integer("id").autoIncrement()
    val amountCents = long("amount_cents")
    val type = enumerationByName<TransactionType>("type", 50)
    val description = varchar("description", 255).default("")
    val date = date("date")
    val budgetItemId = integer("budget_item_id").references(BudgetItems.id)

    override val primaryKey = PrimaryKey(id)
}
