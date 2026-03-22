package dev.jcasas.features.categories

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class CategoryRepository {
    suspend fun create(category: NewCategory): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories.insert {
                it[name] = category.name
                it[type] = category.type
                it[defaultAllocationCents] = category.defaultAllocationCents
            }[Categories.id]
        }

    suspend fun findById(id: Int): Category? =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories
                .selectAll()
                .where { Categories.id eq id }
                .map(ResultRow::toCategory)
                .singleOrNull()
        }

    suspend fun findAllActive(): List<Category> =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories
                .selectAll()
                .where { Categories.active eq true }
                .map(ResultRow::toCategory)
        }

    suspend fun update(
        id: Int,
        category: NewCategory,
    ) = newSuspendedTransaction(Dispatchers.IO) {
        Categories.update({ Categories.id eq id }) {
            it[name] = category.name
            it[type] = category.type
            it[defaultAllocationCents] = category.defaultAllocationCents
        }
    }

    suspend fun softDelete(id: Int) =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories.update({ Categories.id eq id }) {
                it[active] = false
            }
        }

    suspend fun createBatch(categories: List<NewCategory>): List<Int> =
        newSuspendedTransaction(Dispatchers.IO) {
            Categories
                .batchInsert(categories) { category ->
                    this[Categories.name] = category.name
                    this[Categories.type] = category.type
                    this[Categories.defaultAllocationCents] = category.defaultAllocationCents
                }.map { it[Categories.id] }
        }
}

private fun ResultRow.toCategory() =
    Category(
        id = this[Categories.id],
        name = this[Categories.name],
        type = this[Categories.type],
        defaultAllocationCents = this[Categories.defaultAllocationCents],
        active = this[Categories.active],
    )
