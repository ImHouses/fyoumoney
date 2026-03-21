package dev.jcasas.features.budgets

import dev.jcasas.features.categories.Categories
import org.jetbrains.exposed.sql.Table

object Budgets : Table() {
    val id = integer("id").autoIncrement()
    val year = integer("year")
    val month = integer("month")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(year, month)
    }
}

object BudgetItems : Table("budget_items") {
    val id = integer("id").autoIncrement()
    val budgetId = integer("budget_id").references(Budgets.id)
    val categoryId = integer("category_id").references(Categories.id)
    val allocationCents = long("allocation_cents")
    val snoozed = bool("snoozed").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(budgetId, categoryId)
    }
}
