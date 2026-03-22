package dev.jcasas.features.categories

import dev.jcasas.configureSerialization
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

class CategoryRoutesTest {
    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_cat_routes;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureCategoryRoutes(CategoryService(CategoryRepository()))
            }
            block()
        }

    @Test
    fun `POST categories creates and returns 201`() =
        withApp {
            val response =
                client.post("/categories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }

    @Test
    fun `GET categories returns all active categories`() =
        withApp {
            client.post("/categories") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
            }
            val response = client.get("/categories")
            assertEquals(HttpStatusCode.OK, response.status)
            assertContains(response.bodyAsText(), "Food")
        }

    @Test
    fun `GET categories by id returns the category`() =
        withApp {
            val created =
                client.post("/categories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
                }
            val id = created.bodyAsText().trim()
            val response = client.get("/categories/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            assertContains(response.bodyAsText(), "Food")
        }

    @Test
    fun `GET categories by id returns 404 when not found`() =
        withApp {
            val response = client.get("/categories/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `PUT categories updates and returns 200`() =
        withApp {
            val created =
                client.post("/categories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
                }
            val id = created.bodyAsText().trim()
            val response =
                client.put("/categories/$id") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Groceries","type":"EXPENSE","defaultAllocation":"600.00"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `POST categories batch creates multiple and returns 201 with ids`() =
        withApp {
            val response =
                client.post("/categories/batch") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """[
                {"name":"Salary","type":"INCOME","defaultAllocation":"5000.00"},
                {"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"},
                {"name":"Rent","type":"EXPENSE","defaultAllocation":"1200.00"}
            ]""",
                    )
                }
            assertEquals(HttpStatusCode.Created, response.status)
            val body = response.bodyAsText()
            // Response is a JSON array of 3 IDs
            assertContains(body, ",")

            // Verify all 3 categories exist
            val getResponse = client.get("/categories")
            val getBody = getResponse.bodyAsText()
            assertContains(getBody, "Salary")
            assertContains(getBody, "Food")
            assertContains(getBody, "Rent")
        }

    @Test
    fun `DELETE categories soft-deletes and returns 200`() =
        withApp {
            val created =
                client.post("/categories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Food","type":"EXPENSE","defaultAllocation":"500.00"}""")
                }
            val id = created.bodyAsText().trim()
            val response = client.delete("/categories/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            val allResponse = client.get("/categories")
            val body = allResponse.bodyAsText()
            assertEquals("[]", body)
        }
}
