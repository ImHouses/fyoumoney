package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import org.jetbrains.exposed.sql.Table

object Categories : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val type = enumerationByName<TransactionType>("type", 50)
    val defaultAllocationCents = long("default_allocation_cents")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)
}
