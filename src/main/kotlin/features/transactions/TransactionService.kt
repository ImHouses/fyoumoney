package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItem
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.categories.CategoryService

class TransactionService(
    private val repository: TransactionRepository,
    private val budgetService: BudgetService,
    private val categoryService: CategoryService,
) {
    suspend fun create(transaction: NewTransaction): Int {
        val (budgetItem, type) = resolve(transaction)
        return repository.create(transaction, budgetItem.id, type)
    }

    suspend fun getById(id: Int): Transaction? = repository.findById(id)

    suspend fun getAll(
        categoryId: Int? = null,
        year: Int? = null,
        month: Int? = null,
        budgetItemId: Int? = null,
    ): List<Transaction> = repository.findAll(categoryId, year, month, budgetItemId)

    suspend fun update(
        id: Int,
        transaction: NewTransaction,
    ) {
        val (budgetItem, type) = resolve(transaction)
        repository.update(id, transaction, budgetItem.id, type)
    }

    suspend fun delete(id: Int) = repository.delete(id)

    private suspend fun resolve(transaction: NewTransaction): Pair<BudgetItem, TransactionType> {
        val category =
            categoryService.getById(transaction.categoryId)
                ?: throw IllegalArgumentException("Category ${transaction.categoryId} not found")

        val year = transaction.date.year
        val month = transaction.date.monthValue

        val budgetItem =
            budgetService.findBudgetItemForTransaction(transaction.categoryId, year, month)
                ?: throw IllegalArgumentException(
                    "Category ${transaction.categoryId} is not available for month $year-$month",
                )

        return budgetItem to category.type
    }
}
