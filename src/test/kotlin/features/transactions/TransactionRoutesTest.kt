package dev.jcasas.features.transactions

import dev.jcasas.configureSerialization
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.BudgetRepository
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TransactionRoutesTest {
    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_txn_routes_v2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            val categoryService = CategoryService(CategoryRepository())
            val budgetService = BudgetService(BudgetRepository(), categoryService, TransactionRepository())
            val transactionService = TransactionService(TransactionRepository(), budgetService, categoryService)
            application {
                configureSerialization()
                configureCategoryRoutes(categoryService)
                configureTransactionRoutes(transactionService)
            }
            block()
        }

    private suspend fun ApplicationTestBuilder.createCategory(): String {
        val response =
            client.post("/categories") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
            }
        return response.bodyAsText().trim()
    }

    @Test
    fun `POST transactions creates and returns 201`() =
        withApp {
            val categoryId = createCategory()

            val response =
                client.post("/transactions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""",
                    )
                }

            assertEquals(HttpStatusCode.Created, response.status)
        }

    @Test
    fun `GET transactions returns all transactions`() =
        withApp {
            val categoryId = createCategory()
            client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
            }

            val response = client.get("/transactions")

            assertEquals(HttpStatusCode.OK, response.status)
            assertContains(response.bodyAsText(), "Groceries")
            assertContains(response.bodyAsText(), "budgetItemId")
            assertContains(response.bodyAsText(), "categoryId")
        }

    @Test
    fun `GET transactions by id returns the transaction`() =
        withApp {
            val categoryId = createCategory()
            val created =
                client.post("/transactions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""",
                    )
                }
            val id = created.bodyAsText().trim()

            val response = client.get("/transactions/$id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertContains(response.bodyAsText(), "Groceries")
        }

    @Test
    fun `GET transactions by id returns 404 when not found`() =
        withApp {
            val response = client.get("/transactions/999")

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `PUT transactions updates and returns 200`() =
        withApp {
            val categoryId = createCategory()
            val created =
                client.post("/transactions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""",
                    )
                }
            val id = created.bodyAsText().trim()

            val response =
                client.put("/transactions/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":"30.00","categoryId":$categoryId,"description":"Groceries and drinks","date":"2026-03-20"}""",
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `DELETE transactions removes and returns 200`() =
        withApp {
            val categoryId = createCategory()
            val created =
                client.post("/transactions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""",
                    )
                }
            val id = created.bodyAsText().trim()

            val response = client.delete("/transactions/$id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(HttpStatusCode.NotFound, client.get("/transactions/$id").status)
        }

    @Test
    fun `GET transactions supports query filters`() =
        withApp {
            val categoryId = createCategory()
            client.post("/transactions") {
                contentType(ContentType.Application.Json)
                setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
            }

            val response = client.get("/transactions?categoryId=$categoryId&year=2026&month=3")

            assertEquals(HttpStatusCode.OK, response.status)
            assertContains(response.bodyAsText(), "Groceries")
        }

    @Test
    fun `POST transactions returns 422 for soft-deleted category without budget item`() =
        withApp {
            val categoryId = createCategory()
            // Soft-delete the category before any budget is created for the target month
            client.delete("/categories/$categoryId")

            val response =
                client.post("/transactions") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"amount":"10.00","categoryId":$categoryId,"description":"Test","date":"2026-05-01"}""")
                }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }
}
