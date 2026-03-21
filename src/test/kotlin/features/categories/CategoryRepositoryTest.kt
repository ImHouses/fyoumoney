package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryRepositoryTest {

    private lateinit var repository: CategoryRepository

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }
        repository = CategoryRepository()
    }

    @Test
    fun `create stores a category and returns its id`() = runBlocking {
        val id = repository.create(
            NewCategory(name = "Food", type = TransactionType.EXPENSE, defaultAllocationCents = 50000),
        )
        assertTrue(id > 0)
    }

    @Test
    fun `findById returns category with correct fields`() = runBlocking {
        val id = repository.create(
            NewCategory(name = "Salary", type = TransactionType.INCOME, defaultAllocationCents = 300000),
        )
        val found = repository.findById(id)
        assertNotNull(found)
        assertEquals("Salary", found.name)
        assertEquals(TransactionType.INCOME, found.type)
        assertEquals(300000L, found.defaultAllocationCents)
        assertTrue(found.active)
    }

    @Test
    fun `findById returns null when category does not exist`() = runBlocking {
        assertNull(repository.findById(999))
    }

    @Test
    fun `findAllActive returns only active categories`() = runBlocking {
        repository.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val deletedId = repository.create(NewCategory("Old", TransactionType.EXPENSE, 10000))
        repository.softDelete(deletedId)
        val active = repository.findAllActive()
        assertEquals(1, active.size)
        assertEquals("Food", active[0].name)
    }

    @Test
    fun `update changes category fields`() = runBlocking {
        val id = repository.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        repository.update(id, NewCategory("Groceries", TransactionType.EXPENSE, 60000))
        val updated = repository.findById(id)
        assertNotNull(updated)
        assertEquals("Groceries", updated.name)
        assertEquals(60000L, updated.defaultAllocationCents)
    }

    @Test
    fun `softDelete sets active to false`() = runBlocking {
        val id = repository.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        repository.softDelete(id)
        val found = repository.findById(id)
        assertNotNull(found)
        assertFalse(found.active)
    }
}
