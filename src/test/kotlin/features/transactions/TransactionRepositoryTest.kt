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

class TransactionRepositoryTest {

    private lateinit var repository: TransactionRepository

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_txn_repo;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions)
            SchemaUtils.create(Transactions)
        }
        repository = TransactionRepository()
    }

    @Test
    fun `create stores a transaction and returns its id`() = runBlocking {
        val id = repository.create(
            NewTransaction(
                amount = BigDecimal("25.50"),
                type = TransactionType.EXPENSE,
                description = "Grocery shopping",
                date = LocalDate.of(2026, 3, 20),
            ),
        )

        assertTrue(id > 0)
    }

    @Test
    fun `findById returns transaction with correct fields and amount precision`() = runBlocking {
        val id = repository.create(
            NewTransaction(
                amount = BigDecimal("25.50"),
                type = TransactionType.EXPENSE,
                description = "Grocery shopping",
                date = LocalDate.of(2026, 3, 20),
            ),
        )

        val found = repository.findById(id)

        assertNotNull(found)
        assertEquals(id, found.id)
        assertEquals(BigDecimal("25.50"), found.amount)
        assertEquals(TransactionType.EXPENSE, found.type)
        assertEquals("Grocery shopping", found.description)
        assertEquals(LocalDate.of(2026, 3, 20), found.date)
    }

    @Test
    fun `findById returns null when transaction does not exist`() = runBlocking {
        val found = repository.findById(999)

        assertNull(found)
    }

    @Test
    fun `findAll returns all stored transactions`() = runBlocking {
        repository.create(
            NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)),
        )
        repository.create(
            NewTransaction(BigDecimal("1500.00"), TransactionType.INCOME, "Salary", LocalDate.of(2026, 3, 20)),
        )

        val all = repository.findAll()

        assertEquals(2, all.size)
    }

    @Test
    fun `update changes the transaction fields`() = runBlocking {
        val id = repository.create(
            NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        repository.update(
            id,
            NewTransaction(BigDecimal("12.50"), TransactionType.EXPENSE, "Coffee and cake", LocalDate.of(2026, 3, 21)),
        )

        val updated = repository.findById(id)
        assertNotNull(updated)
        assertEquals(BigDecimal("12.50"), updated.amount)
        assertEquals("Coffee and cake", updated.description)
        assertEquals(LocalDate.of(2026, 3, 21), updated.date)
    }

    @Test
    fun `delete removes the transaction`() = runBlocking {
        val id = repository.create(
            NewTransaction(BigDecimal("10.00"), TransactionType.EXPENSE, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        repository.delete(id)

        assertNull(repository.findById(id))
    }
}