package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
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
    private var budgetItemId: Int = 0
    private var categoryId: Int = 0

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_txn_repo_v2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)

            // Create prerequisite data
            val catId =
                Categories.insert {
                    it[name] = "Food"
                    it[type] = TransactionType.EXPENSE
                    it[defaultAllocationCents] = 50000
                    it[active] = true
                }[Categories.id]
            categoryId = catId

            val budgetId =
                Budgets.insert {
                    it[year] = 2026
                    it[month] = 3
                }[Budgets.id]

            budgetItemId =
                BudgetItems.insert {
                    it[BudgetItems.budgetId] = budgetId
                    it[BudgetItems.categoryId] = catId
                    it[allocationCents] = 50000
                    it[snoozed] = false
                }[BudgetItems.id]
        }
        repository = TransactionRepository()
    }

    @Test
    fun `create stores a transaction and returns its id`() =
        runBlocking {
            val id =
                repository.create(
                    NewTransaction(
                        amount = BigDecimal("25.50"),
                        categoryId = categoryId,
                        description = "Grocery shopping",
                        date = LocalDate.of(2026, 3, 20),
                    ),
                    budgetItemId = budgetItemId,
                    type = TransactionType.EXPENSE,
                )

            assertTrue(id > 0)
        }

    @Test
    fun `findById returns transaction with correct fields`() =
        runBlocking {
            val id =
                repository.create(
                    NewTransaction(
                        amount = BigDecimal("25.50"),
                        categoryId = categoryId,
                        description = "Grocery shopping",
                        date = LocalDate.of(2026, 3, 20),
                    ),
                    budgetItemId = budgetItemId,
                    type = TransactionType.EXPENSE,
                )

            val found = repository.findById(id)

            assertNotNull(found)
            assertEquals(id, found.id)
            assertEquals(BigDecimal("25.50"), found.amount)
            assertEquals(TransactionType.EXPENSE, found.type)
            assertEquals("Grocery shopping", found.description)
            assertEquals(LocalDate.of(2026, 3, 20), found.date)
            assertEquals(budgetItemId, found.budgetItemId)
            assertEquals(categoryId, found.categoryId)
        }

    @Test
    fun `findById returns null when transaction does not exist`() =
        runBlocking {
            assertNull(repository.findById(999))
        }

    @Test
    fun `findAll returns all stored transactions`() =
        runBlocking {
            repository.create(
                NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
                budgetItemId,
                TransactionType.EXPENSE,
            )
            repository.create(
                NewTransaction(BigDecimal("15.00"), categoryId, "Lunch", LocalDate.of(2026, 3, 20)),
                budgetItemId,
                TransactionType.EXPENSE,
            )

            val all = repository.findAll()

            assertEquals(2, all.size)
        }

    @Test
    fun `update changes the transaction fields`() =
        runBlocking {
            val id =
                repository.create(
                    NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
                    budgetItemId,
                    TransactionType.EXPENSE,
                )

            repository.update(
                id,
                NewTransaction(BigDecimal("12.50"), categoryId, "Coffee and cake", LocalDate.of(2026, 3, 21)),
                budgetItemId,
                TransactionType.EXPENSE,
            )

            val updated = repository.findById(id)
            assertNotNull(updated)
            assertEquals(BigDecimal("12.50"), updated.amount)
            assertEquals("Coffee and cake", updated.description)
        }

    @Test
    fun `delete removes the transaction`() =
        runBlocking {
            val id =
                repository.create(
                    NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
                    budgetItemId,
                    TransactionType.EXPENSE,
                )

            repository.delete(id)

            assertNull(repository.findById(id))
        }
}
