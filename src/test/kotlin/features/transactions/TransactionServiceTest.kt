package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionServiceTest {

    private lateinit var service: TransactionService

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_txn_svc;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions)
            SchemaUtils.create(Transactions)
        }
        service = TransactionService(TransactionRepository())
    }

    @Test
    fun `create returns the id of the new transaction`() = runBlocking {
        val id = service.create(
            NewTransaction(
                amount = BigDecimal("100.00"),
                type = TransactionType.INCOME,
                description = "Freelance payment",
                date = LocalDate.of(2026, 3, 20),
            ),
        )

        assertTrue(id > 0)
    }

    @Test
    fun `getById returns the transaction`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("50.00"), TransactionType.EXPENSE, "Dinner", LocalDate.of(2026, 3, 20)),
        )

        val transaction = service.getById(id)

        assertNotNull(transaction)
        assertEquals(BigDecimal("50.00"), transaction.amount)
        assertEquals("Dinner", transaction.description)
    }

    @Test
    fun `getById returns null when transaction does not exist`() = runBlocking {
        assertNull(service.getById(999))
    }

    @Test
    fun `getAll returns all transactions`() = runBlocking {
        service.create(NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)))
        service.create(NewTransaction(BigDecimal("200.00"), TransactionType.INCOME, "Invoice", LocalDate.of(2026, 3, 20)))

        val all = service.getAll()

        assertEquals(2, all.size)
    }

    @Test
    fun `update modifies the transaction`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        service.update(id, NewTransaction(BigDecimal("15.00"), TransactionType.EXPENSE, "Coffee and tip", LocalDate.of(2026, 3, 20)))

        val updated = service.getById(id)
        assertNotNull(updated)
        assertEquals(BigDecimal("15.00"), updated.amount)
        assertEquals("Coffee and tip", updated.description)
    }

    @Test
    fun `delete removes the transaction`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        service.delete(id)

        assertNull(service.getById(id))
    }
}