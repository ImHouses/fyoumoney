package dev.jcasas

import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
import dev.jcasas.features.transactions.TransactionService
import dev.jcasas.features.transactions.configureTransactionRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
    val transactionService: TransactionService by inject()
    configureTransactionRoutes(transactionService)
    val categoryService: CategoryService by inject()
    configureCategoryRoutes(categoryService)
}