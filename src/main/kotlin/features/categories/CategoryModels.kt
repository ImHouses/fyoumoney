package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import kotlinx.serialization.Serializable

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
    val defaultAllocationCents: Long,
) {
    fun toNewCategory() = NewCategory(
        name = name,
        type = type,
        defaultAllocationCents = defaultAllocationCents,
    )
}

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val type: TransactionType,
    val defaultAllocationCents: Long,
    val active: Boolean,
) {
    companion object {
        fun from(category: Category) = CategoryResponse(
            id = category.id,
            name = category.name,
            type = category.type,
            defaultAllocationCents = category.defaultAllocationCents,
            active = category.active,
        )
    }
}
