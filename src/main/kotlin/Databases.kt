package dev.jcasas

import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.transactions.Transactions
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    Database.connect(url = url, user = user, password = password, driver = "org.postgresql.Driver")

    val isDevelopment =
        environment.config
            .propertyOrNull("ktor.development")
            ?.getString()
            ?.toBoolean() ?: false

    if (isDevelopment) {
        transaction {
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
    }
}
