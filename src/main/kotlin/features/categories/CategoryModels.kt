package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import kotlinx.serialization.Serializable
import java.math.BigDecimal

data class Category(
    val id: Int,
    val name: String,
    val type: TransactionType,
    val defaultAllocationCents: Long,
    val active: Boolean,
)

data class NewCategory(
    val name: String,
    val type: TransactionType,
    val defaultAllocationCents: Long,
)

@Serializable
data class CategoryRequest(
    val name: String,
    val type: TransactionType,
    val defaultAllocation: String,
) {
    fun toNewCategory() = NewCategory(
        name = name,
        type = type,
        defaultAllocationCents = BigDecimal(defaultAllocation).movePointRight(2).toLong(),
    )
}

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val type: TransactionType,
    val defaultAllocation: String,
    val active: Boolean,
) {
    companion object {
        fun from(category: Category) = CategoryResponse(
            id = category.id,
            name = category.name,
            type = category.type,
            defaultAllocation = BigDecimal(category.defaultAllocationCents).movePointLeft(2).toPlainString(),
            active = category.active,
        )
    }
}
