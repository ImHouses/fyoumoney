package dev.jcasas.features.transactions

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Transactions : Table() {
    val id = integer("id").autoIncrement()
    val amountCents = long("amount_cents")
    val type = enumerationByName<TransactionType>("type", 50)
    val description = varchar("description", 255)
    val date = date("date")

    override val primaryKey = PrimaryKey(id)
}