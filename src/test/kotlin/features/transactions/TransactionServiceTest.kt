package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.BudgetRepository
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.NewCategory
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
    private lateinit var categoryService: CategoryService
    private lateinit var budgetService: BudgetService
    private var foodCategoryId: Int = 0

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_txn_svc_v2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
        categoryService = CategoryService(CategoryRepository())
        budgetService = BudgetService(BudgetRepository(), categoryService)
        service = TransactionService(TransactionRepository(), budgetService, categoryService)

        runBlocking {
            foodCategoryId =
                categoryService.create(
                    NewCategory("Food", TransactionType.EXPENSE, 50000),
                )
        }
    }

    @Test
    fun `create resolves budget item and returns id`() =
        runBlocking {
            val id =
                service.create(
                    NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 3, 20)),
                )

            assertTrue(id > 0)
        }

    @Test
    fun `create auto-creates budget when none exists`() =
        runBlocking {
            val id =
                service.create(
                    NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 6, 15)),
                )

            val transaction = service.getById(id)
            assertNotNull(transaction)
            assertEquals(foodCategoryId, transaction.categoryId)
        }

    @Test
    fun `create infers type from category`() =
        runBlocking {
            val id =
                service.create(
                    NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 3, 20)),
                )

            val transaction = service.getById(id)
            assertNotNull(transaction)
            assertEquals(TransactionType.EXPENSE, transaction.type)
        }

    @Test
    fun `getById returns null when not found`() =
        runBlocking {
            assertNull(service.getById(999))
        }

    @Test
    fun `getAll returns all transactions`() =
        runBlocking {
            service.create(NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)))
            service.create(NewTransaction(BigDecimal("20.00"), foodCategoryId, "Lunch", LocalDate.of(2026, 3, 20)))

            val all = service.getAll()

            assertEquals(2, all.size)
        }

    @Test
    fun `update re-resolves budget item when date changes month`() =
        runBlocking {
            val id =
                service.create(
                    NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)),
                )

            service.update(id, NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 4, 1)))

            val updated = service.getById(id)
            assertNotNull(updated)
            assertEquals(LocalDate.of(2026, 4, 1), updated.date)
        }

    @Test
    fun `delete removes the transaction`() =
        runBlocking {
            val id =
                service.create(
                    NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)),
                )

            service.delete(id)

            assertNull(service.getById(id))
        }

    @Test
    fun `create rejects transaction for soft-deleted category without budget item`() =
        runBlocking {
            val oldCategoryId =
                categoryService.create(
                    NewCategory("OldStuff", TransactionType.EXPENSE, 10000),
                )
            categoryService.delete(oldCategoryId)

            var threw = false
            try {
                service.create(
                    NewTransaction(BigDecimal("10.00"), oldCategoryId, "Test", LocalDate.of(2026, 5, 1)),
                )
            } catch (e: IllegalArgumentException) {
                threw = true
            }

            assertTrue(threw)
        }

    @Test
    fun `create allows transaction for soft-deleted category with existing budget item`() =
        runBlocking {
            val oldCategoryId =
                categoryService.create(
                    NewCategory("OldStuff", TransactionType.EXPENSE, 10000),
                )
            // Create budget while category is still active — this creates its budget item
            budgetService.getOrCreateBudget(2026, 5)
            // Now soft-delete the category
            categoryService.delete(oldCategoryId)

            val id =
                service.create(
                    NewTransaction(BigDecimal("10.00"), oldCategoryId, "Test", LocalDate.of(2026, 5, 1)),
                )

            assertTrue(id > 0)
        }
}
