package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.NewCategory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetRepositoryTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var categoryRepository: CategoryRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems)
        }
        budgetRepository = BudgetRepository()
        categoryRepository = CategoryRepository()
    }

    @Test
    fun `createBudgetWithItems stores budget and items atomically`() = runBlocking {
        val categoryId = categoryRepository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )
        val budget = budgetRepository.createBudgetWithItems(
            2026, 3, listOf(categoryId to 50000L),
        )
        assertTrue(budget.id > 0)
        assertEquals(2026, budget.year)
        assertEquals(3, budget.month)
    }

    @Test
    fun `findBudgetByYearMonth returns the budget`() = runBlocking {
        budgetRepository.createBudgetWithItems(2026, 3, emptyList())
        val found = budgetRepository.findBudgetByYearMonth(2026, 3)
        assertNotNull(found)
        assertEquals(2026, found.year)
        assertEquals(3, found.month)
    }

    @Test
    fun `findBudgetByYearMonth returns null when not found`() = runBlocking {
        assertNull(budgetRepository.findBudgetByYearMonth(2026, 3))
    }

    @Test
    fun `findBudgetItemByBudgetAndCategory returns the item`() = runBlocking {
        val categoryId = categoryRepository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )
        val budget = budgetRepository.createBudgetWithItems(
            2026, 3, listOf(categoryId to 50000L),
        )
        val found = budgetRepository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)
        assertNotNull(found)
        assertEquals(categoryId, found.categoryId)
        assertEquals(50000L, found.allocationCents)
        assertEquals(false, found.snoozed)
    }

    @Test
    fun `updateBudgetItem changes allocation and snoozed`() = runBlocking {
        val categoryId = categoryRepository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )
        val budget = budgetRepository.createBudgetWithItems(
            2026, 3, listOf(categoryId to 50000L),
        )
        val item = budgetRepository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)!!
        budgetRepository.updateBudgetItem(item.id, allocationCents = 60000, snoozed = true)
        val updated = budgetRepository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)
        assertNotNull(updated)
        assertEquals(60000L, updated.allocationCents)
        assertEquals(true, updated.snoozed)
    }

    @Test
    fun `findAllBudgets returns all budgets`() = runBlocking {
        budgetRepository.createBudgetWithItems(2026, 1, emptyList())
        budgetRepository.createBudgetWithItems(2026, 2, emptyList())
        budgetRepository.createBudgetWithItems(2025, 12, emptyList())
        val all = budgetRepository.findAllBudgets(year = null)
        assertEquals(3, all.size)
    }

    @Test
    fun `findAllBudgets filters by year`() = runBlocking {
        budgetRepository.createBudgetWithItems(2026, 1, emptyList())
        budgetRepository.createBudgetWithItems(2026, 2, emptyList())
        budgetRepository.createBudgetWithItems(2025, 12, emptyList())
        val filtered = budgetRepository.findAllBudgets(year = 2026)
        assertEquals(2, filtered.size)
    }

    @Test
    fun `findBudgetItemById returns the item`() = runBlocking {
        val categoryId = categoryRepository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )
        val budget = budgetRepository.createBudgetWithItems(
            2026, 3, listOf(categoryId to 50000L),
        )
        val item = budgetRepository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)!!
        val found = budgetRepository.findBudgetItemById(item.id)
        assertNotNull(found)
        assertEquals(categoryId, found.categoryId)
    }
}
