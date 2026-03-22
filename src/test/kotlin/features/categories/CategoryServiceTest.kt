package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryServiceTest {
    private lateinit var service: CategoryService

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:test_cat_svc;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }
        service = CategoryService(CategoryRepository())
    }

    @Test
    fun `create returns the id of the new category`() =
        runBlocking {
            val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
            assertTrue(id > 0)
        }

    @Test
    fun `getById returns the category`() =
        runBlocking {
            val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
            val category = service.getById(id)
            assertNotNull(category)
            assertEquals("Food", category.name)
        }

    @Test
    fun `getById returns null when not found`() =
        runBlocking {
            assertNull(service.getById(999))
        }

    @Test
    fun `getAllActive excludes soft-deleted categories`() =
        runBlocking {
            service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
            val deletedId = service.create(NewCategory("Old", TransactionType.EXPENSE, 10000))
            service.delete(deletedId)
            val active = service.getAllActive()
            assertEquals(1, active.size)
        }

    @Test
    fun `update changes category fields`() =
        runBlocking {
            val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
            service.update(id, NewCategory("Groceries", TransactionType.EXPENSE, 60000))
            val updated = service.getById(id)
            assertNotNull(updated)
            assertEquals("Groceries", updated.name)
        }

    @Test
    fun `delete soft-deletes the category`() =
        runBlocking {
            val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
            service.delete(id)
            val found = service.getById(id)
            assertNotNull(found)
            assertFalse(found.active)
        }
}
