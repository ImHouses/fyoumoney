package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.NewCategory
import dev.jcasas.features.transactions.Transactions
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BudgetServiceTest {

    private lateinit var budgetService: BudgetService
    private lateinit var categoryService: CategoryService

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_budget_svc;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
        categoryService = CategoryService(CategoryRepository())
        budgetService = BudgetService(BudgetRepository(), categoryService)
    }

    @Test
    fun `getOrCreateBudget creates budget with all active categories`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        categoryService.create(NewCategory("Salary", TransactionType.INCOME, 300000))
        val response = budgetService.getOrCreateBudget(2026, 3)
        assertEquals(2026, response.year)
        assertEquals(3, response.month)
        assertEquals(2, response.items.size)
    }

    @Test
    fun `getOrCreateBudget returns existing budget on second call`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val first = budgetService.getOrCreateBudget(2026, 3)
        val second = budgetService.getOrCreateBudget(2026, 3)
        assertEquals(first.id, second.id)
        assertEquals(1, second.items.size)
    }

    @Test
    fun `getOrCreateBudget excludes soft-deleted categories`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val deletedId = categoryService.create(NewCategory("Old", TransactionType.EXPENSE, 10000))
        categoryService.delete(deletedId)
        val response = budgetService.getOrCreateBudget(2026, 3)
        assertEquals(1, response.items.size)
        assertEquals("Food", response.items[0].categoryName)
    }

    @Test
    fun `getOrCreateBudget copies default allocation from category`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val response = budgetService.getOrCreateBudget(2026, 3)
        assertEquals("500.00", response.items[0].allocation)
    }

    @Test
    fun `getOrCreateBudget returns spent totals as zero when no transactions`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val response = budgetService.getOrCreateBudget(2026, 3)
        assertEquals("0.00", response.items[0].spent)
    }

    @Test
    fun `updateBudgetItem changes allocation`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val budget = budgetService.getOrCreateBudget(2026, 3)
        val itemId = budget.items[0].id
        budgetService.updateBudgetItem(itemId, allocationCents = 60000, snoozed = null)
        val updated = budgetService.getOrCreateBudget(2026, 3)
        assertEquals("600.00", updated.items[0].allocation)
    }

    @Test
    fun `updateBudgetItem changes snoozed flag`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val budget = budgetService.getOrCreateBudget(2026, 3)
        val itemId = budget.items[0].id
        budgetService.updateBudgetItem(itemId, allocationCents = null, snoozed = true)
        val updated = budgetService.getOrCreateBudget(2026, 3)
        assertTrue(updated.items[0].snoozed)
    }

    @Test
    fun `getAllBudgets returns all budgets`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        budgetService.getOrCreateBudget(2026, 1)
        budgetService.getOrCreateBudget(2026, 2)
        val all = budgetService.getAllBudgets(year = null)
        assertEquals(2, all.size)
    }

    @Test
    fun `getAllBudgets filters by year`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        budgetService.getOrCreateBudget(2026, 1)
        budgetService.getOrCreateBudget(2025, 12)
        val filtered = budgetService.getAllBudgets(year = 2026)
        assertEquals(1, filtered.size)
    }

    @Test
    fun `findBudgetItemForTransaction returns existing item`() = runBlocking {
        val categoryId = categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        budgetService.getOrCreateBudget(2026, 3)
        val item = budgetService.findBudgetItemForTransaction(categoryId, 2026, 3)
        assertNotNull(item)
        assertEquals(categoryId, item.categoryId)
    }

    @Test
    fun `findBudgetItemForTransaction auto-creates budget if needed`() = runBlocking {
        val categoryId = categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val item = budgetService.findBudgetItemForTransaction(categoryId, 2026, 3)
        assertNotNull(item)
        assertEquals(categoryId, item.categoryId)
    }
}