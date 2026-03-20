package dev.jcasas.features.transactions

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import dev.jcasas.configureSerialization
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TransactionRoutesTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Transactions)
            SchemaUtils.create(Transactions)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureTransactionRoutes(TransactionService(TransactionRepository()))
            }
            block()
        }

    @Test
    fun `POST transactions creates a transaction and returns 201`() = withApp {
        val response = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","type":"EXPENSE","description":"Groceries","date":"2026-03-20"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET transactions returns all transactions`() = withApp {
        client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","type":"EXPENSE","description":"Groceries","date":"2026-03-20"}""")
        }

        val response = client.get("/transactions")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Groceries")
    }

    @Test
    fun `GET transactions by id returns the transaction`() = withApp {
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","type":"EXPENSE","description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.get("/transactions/$id")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Groceries")
    }

    @Test
    fun `GET transactions by id returns 404 when not found`() = withApp {
        val response = client.get("/transactions/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT transactions updates and returns 200`() = withApp {
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","type":"EXPENSE","description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.put("/transactions/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"30.00","type":"EXPENSE","description":"Groceries and drinks","date":"2026-03-20"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE transactions removes and returns 200`() = withApp {
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","type":"EXPENSE","description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.delete("/transactions/$id")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/transactions/$id").status)
    }
}