package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import kotlinx.serialization.Serializable

data class Budget(
    val id: Int,
    val year: Int,
    val month: Int,
)

data class BudgetItem(
    val id: Int,
    val budgetId: Int,
    val categoryId: Int,
    val allocationCents: Long,
    val snoozed: Boolean,
)

@Serializable
data class BudgetItemUpdateRequest(
    val allocation: String? = null,
    val snoozed: Boolean? = null,
)

@Serializable
data class BudgetItemResponse(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryType: TransactionType,
    val allocation: String,
    val spent: String,
    val snoozed: Boolean,
)

@Serializable
data class BudgetResponse(
    val id: Int,
    val year: Int,
    val month: Int,
    val items: List<BudgetItemResponse>,
)

@Serializable
data class BudgetSummaryResponse(
    val id: Int,
    val year: Int,
    val month: Int,
)
