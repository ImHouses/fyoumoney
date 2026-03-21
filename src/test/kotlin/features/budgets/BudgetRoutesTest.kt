package dev.jcasas.features.budgets

import dev.jcasas.configureSerialization
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
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

class BudgetRoutesTest {

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_budget_routes;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            val categoryService = CategoryService(CategoryRepository())
            val budgetService = BudgetService(BudgetRepository(), categoryService)
            application {
                configureSerialization()
                configureCategoryRoutes(categoryService)
                configureBudgetRoutes(budgetService)
            }
            block()
        }

    @Test
    fun `GET budgets year month auto-creates and returns budget`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val response = client.get("/budgets/2026/3")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "Food")
        assertContains(body, "50000")
    }

    @Test
    fun `GET budgets year month returns 400 for invalid year or month`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/budgets/abc/3").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/budgets/2026/abc").status)
    }

    @Test
    fun `GET budgets returns list of all budgets`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        client.get("/budgets/2026/1")
        client.get("/budgets/2026/2")
        val response = client.get("/budgets")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "2026")
    }

    @Test
    fun `GET budgets filters by year query param`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        client.get("/budgets/2026/1")
        client.get("/budgets/2025/12")
        val response = client.get("/budgets?year=2026")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "2026")
    }

    @Test
    fun `PUT budget item updates allocation`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val budgetResponse = client.get("/budgets/2026/3")
        val body = budgetResponse.bodyAsText()
        val itemIdMatch = Regex(""""items":\[.*?"id":(\d+)""").find(body)!!
        val itemId = itemIdMatch.groupValues[1]
        val response = client.put("/budgets/2026/3/items/$itemId") {
            contentType(ContentType.Application.Json)
            setBody("""{"allocationCents":60000}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
