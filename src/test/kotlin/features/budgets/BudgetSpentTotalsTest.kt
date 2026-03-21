package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.NewCategory
import dev.jcasas.features.transactions.NewTransaction
import dev.jcasas.features.transactions.TransactionRepository
import dev.jcasas.features.transactions.TransactionService
import dev.jcasas.features.transactions.Transactions
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

class BudgetSpentTotalsTest {

    private lateinit var budgetService: BudgetService
    private lateinit var transactionService: TransactionService
    private lateinit var categoryService: CategoryService

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_budget_spent;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
        categoryService = CategoryService(CategoryRepository())
        budgetService = BudgetService(BudgetRepository(), categoryService)
        transactionService = TransactionService(TransactionRepository(), budgetService, categoryService)
    }

    @Test
    fun `spent totals reflect transaction amounts`() = runBlocking {
        val categoryId = categoryService.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )

        transactionService.create(
            NewTransaction(BigDecimal("25.50"), categoryId, "Groceries", LocalDate.of(2026, 3, 20)),
        )
        transactionService.create(
            NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 21)),
        )

        val budget = budgetService.getOrCreateBudget(2026, 3)
        val foodItem = budget.items.first { it.categoryId == categoryId }

        assertEquals(3550L, foodItem.spentCents)
    }

    @Test
    fun `spent totals are zero when no transactions exist`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))

        val budget = budgetService.getOrCreateBudget(2026, 3)

        assertEquals(0L, budget.items[0].spentCents)
    }
}
