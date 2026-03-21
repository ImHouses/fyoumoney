# Budget Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the budget system — categories, monthly budgets with auto-creation, and transaction-to-budget-item linking — as described in `docs/superpowers/specs/2026-03-20-budget-feature-design.md`.

**Architecture:** Three features built in dependency order: Categories (standalone) → Budgets (depends on CategoryService) → Transactions (modified to depend on BudgetService). Each feature follows the existing vertical-slice pattern (Models → Schema → Repository → Service → Routes → Module). Cross-feature access is through services only, with documented exceptions:
1. `TransactionSchema.kt` references `BudgetItemsTable` for the FK constraint (per spec).
2. `TransactionRepository.kt` joins to `BudgetItems` to derive `categoryId` — a natural extension of the FK relationship above.
3. `BudgetService.kt` queries `Transactions` table for spent totals — necessary because the reverse service dependency (BudgetService → TransactionService) would create a circular DI dependency (TransactionService already depends on BudgetService).

**Tech Stack:** Kotlin 2.3.0, Ktor 3.4.1, Exposed 0.61.0, Koin 4.2.0, H2 (tests), kotlinx-serialization

**Spec:** `docs/superpowers/specs/2026-03-20-budget-feature-design.md`

---

## File Structure

### Shared

- **Move:** `TransactionType` enum from `features/transactions/TransactionModels.kt` → `Models.kt` (package root `dev.jcasas`)
  - Reason: `TransactionType` is used by both Categories and Transactions. Extracting it avoids a cross-feature model dependency.

### Categories Feature (new)

| File | Responsibility |
|---|---|
| `features/categories/CategoryModels.kt` | `Category`, `NewCategory`, `CategoryRequest`, `CategoryResponse` data classes |
| `features/categories/CategorySchema.kt` | `Categories` Exposed table |
| `features/categories/CategoryRepository.kt` | CRUD + soft delete + findAllActive |
| `features/categories/CategoryService.kt` | Pass-through + business rules |
| `features/categories/CategoryRoutes.kt` | REST endpoints for `/categories` |
| `features/categories/CategoryModule.kt` | Koin bindings |

### Budgets Feature (new)

| File | Responsibility |
|---|---|
| `features/budgets/BudgetModels.kt` | `Budget`, `BudgetItem`, `BudgetItemResponse`, `BudgetResponse`, update request |
| `features/budgets/BudgetSchema.kt` | `Budgets` and `BudgetItems` Exposed tables |
| `features/budgets/BudgetRepository.kt` | Budget + BudgetItem CRUD, spent totals query |
| `features/budgets/BudgetService.kt` | Auto-creation logic, delegates to repo |
| `features/budgets/BudgetRoutes.kt` | REST endpoints for `/budgets` |
| `features/budgets/BudgetModule.kt` | Koin bindings |

### Transactions Feature (modified)

| File | Change |
|---|---|
| `features/transactions/TransactionModels.kt` | Add `budgetItemId` + `categoryId` to models, remove `type` from request |
| `features/transactions/TransactionSchema.kt` | Add `budgetItemId` FK column referencing `BudgetItems` |
| `features/transactions/TransactionRepository.kt` | Update CRUD for new column, add filtered queries |
| `features/transactions/TransactionService.kt` | Add BudgetService dependency, budget-item resolution logic |
| `features/transactions/TransactionRoutes.kt` | Update request/response shapes, add query filters |
| `features/transactions/TransactionModule.kt` | Update DI to inject BudgetService |

### Wiring (modified)

| File | Change |
|---|---|
| `Frameworks.kt` | Add `categoryModule`, `budgetModule` |
| `Routing.kt` | Add `configureCategoryRoutes`, `configureBudgetRoutes` |

---

## Task 1: Extract Shared TransactionType Enum

**Files:**
- Create: `src/main/kotlin/Models.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionModels.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionSchema.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionRepository.kt`

This task has no tests — it's a mechanical refactor. Existing tests validate nothing breaks.

- [ ] **Step 1: Create shared Models.kt with TransactionType**

Create `src/main/kotlin/Models.kt`:

```kotlin
package dev.jcasas

enum class TransactionType { INCOME, EXPENSE }
```

- [ ] **Step 2: Remove TransactionType from TransactionModels.kt and import from shared**

In `src/main/kotlin/features/transactions/TransactionModels.kt`:
- Remove `enum class TransactionType { INCOME, EXPENSE }`
- Add `import dev.jcasas.TransactionType`

- [ ] **Step 3: Update TransactionSchema.kt import**

In `src/main/kotlin/features/transactions/TransactionSchema.kt`:
- Add `import dev.jcasas.TransactionType`

- [ ] **Step 4: Update TransactionRepository.kt import**

In `src/main/kotlin/features/transactions/TransactionRepository.kt`:
- Add `import dev.jcasas.TransactionType` (needed indirectly through NewTransaction — verify compilation)

- [ ] **Step 5: Run existing tests to verify refactor**

Run: `./gradlew test`
Expected: All 19 tests pass

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/Models.kt src/main/kotlin/features/transactions/TransactionModels.kt src/main/kotlin/features/transactions/TransactionSchema.kt src/main/kotlin/features/transactions/TransactionRepository.kt
git commit -m "refactor: extract TransactionType enum to shared Models.kt"
```

---

## Task 2: Category Models and Schema

**Files:**
- Create: `src/main/kotlin/features/categories/CategoryModels.kt`
- Create: `src/main/kotlin/features/categories/CategorySchema.kt`

No tests yet — models and schema are tested through the repository in Task 3.

- [ ] **Step 1: Create CategoryModels.kt**

Create `src/main/kotlin/features/categories/CategoryModels.kt`:

```kotlin
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
```

- [ ] **Step 2: Create CategorySchema.kt**

Create `src/main/kotlin/features/categories/CategorySchema.kt`:

```kotlin
package dev.jcasas.features.categories

import dev.jcasas.TransactionType
import org.jetbrains.exposed.sql.Table

object Categories : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val type = enumerationByName<TransactionType>("type", 50)
    val defaultAllocationCents = long("default_allocation_cents")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/features/categories/
git commit -m "feat: add Category models and schema"
```

---

## Task 3: Category Repository

**Files:**
- Create: `src/main/kotlin/features/categories/CategoryRepository.kt`
- Create: `src/test/kotlin/features/categories/CategoryRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Create `src/test/kotlin/features/categories/CategoryRepositoryTest.kt`:

```kotlin
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
        val id = repository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )

        repository.update(id, NewCategory("Groceries", TransactionType.EXPENSE, 60000))

        val updated = repository.findById(id)
        assertNotNull(updated)
        assertEquals("Groceries", updated.name)
        assertEquals(60000L, updated.defaultAllocationCents)
    }

    @Test
    fun `softDelete sets active to false`() = runBlocking {
        val id = repository.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )

        repository.softDelete(id)

        val found = repository.findById(id)
        assertNotNull(found)
        assertFalse(found.active)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRepositoryTest"`
Expected: FAIL — `CategoryRepository` not found

- [ ] **Step 3: Implement CategoryRepository**

Create `src/main/kotlin/features/categories/CategoryRepository.kt`:

```kotlin
package dev.jcasas.features.categories

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class CategoryRepository {

    suspend fun create(category: NewCategory): Int = newSuspendedTransaction(Dispatchers.IO) {
        Categories.insert {
            it[name] = category.name
            it[type] = category.type
            it[defaultAllocationCents] = category.defaultAllocationCents
        }[Categories.id]
    }

    suspend fun findById(id: Int): Category? = newSuspendedTransaction(Dispatchers.IO) {
        Categories.selectAll()
            .where { Categories.id eq id }
            .map { row ->
                Category(
                    id = row[Categories.id],
                    name = row[Categories.name],
                    type = row[Categories.type],
                    defaultAllocationCents = row[Categories.defaultAllocationCents],
                    active = row[Categories.active],
                )
            }
            .singleOrNull()
    }

    suspend fun findAllActive(): List<Category> = newSuspendedTransaction(Dispatchers.IO) {
        Categories.selectAll()
            .where { Categories.active eq true }
            .map { row ->
                Category(
                    id = row[Categories.id],
                    name = row[Categories.name],
                    type = row[Categories.type],
                    defaultAllocationCents = row[Categories.defaultAllocationCents],
                    active = row[Categories.active],
                )
            }
    }

    suspend fun update(id: Int, category: NewCategory) = newSuspendedTransaction(Dispatchers.IO) {
        Categories.update({ Categories.id eq id }) {
            it[name] = category.name
            it[type] = category.type
            it[defaultAllocationCents] = category.defaultAllocationCents
        }
    }

    suspend fun softDelete(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Categories.update({ Categories.id eq id }) {
            it[active] = false
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRepositoryTest"`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/categories/CategoryRepository.kt src/test/kotlin/features/categories/CategoryRepositoryTest.kt
git commit -m "feat: add CategoryRepository with CRUD and soft delete"
```

---

## Task 4: Category Service

**Files:**
- Create: `src/main/kotlin/features/categories/CategoryService.kt`
- Create: `src/test/kotlin/features/categories/CategoryServiceTest.kt`

- [ ] **Step 1: Write failing service tests**

Create `src/test/kotlin/features/categories/CategoryServiceTest.kt`:

```kotlin
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

class CategoryServiceTest {

    private lateinit var service: CategoryService

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }
        service = CategoryService(CategoryRepository())
    }

    @Test
    fun `create returns the id of the new category`() = runBlocking {
        val id = service.create(
            NewCategory("Food", TransactionType.EXPENSE, 50000),
        )

        assertTrue(id > 0)
    }

    @Test
    fun `getById returns the category`() = runBlocking {
        val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))

        val category = service.getById(id)

        assertNotNull(category)
        assertEquals("Food", category.name)
    }

    @Test
    fun `getById returns null when not found`() = runBlocking {
        assertNull(service.getById(999))
    }

    @Test
    fun `getAllActive excludes soft-deleted categories`() = runBlocking {
        service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val deletedId = service.create(NewCategory("Old", TransactionType.EXPENSE, 10000))
        service.delete(deletedId)

        val active = service.getAllActive()

        assertEquals(1, active.size)
    }

    @Test
    fun `update changes category fields`() = runBlocking {
        val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))

        service.update(id, NewCategory("Groceries", TransactionType.EXPENSE, 60000))

        val updated = service.getById(id)
        assertNotNull(updated)
        assertEquals("Groceries", updated.name)
    }

    @Test
    fun `delete soft-deletes the category`() = runBlocking {
        val id = service.create(NewCategory("Food", TransactionType.EXPENSE, 50000))

        service.delete(id)

        val found = service.getById(id)
        assertNotNull(found)
        assertFalse(found.active)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryServiceTest"`
Expected: FAIL — `CategoryService` not found

- [ ] **Step 3: Implement CategoryService**

Create `src/main/kotlin/features/categories/CategoryService.kt`:

```kotlin
package dev.jcasas.features.categories

class CategoryService(private val repository: CategoryRepository) {

    suspend fun create(category: NewCategory): Int = repository.create(category)

    suspend fun getById(id: Int): Category? = repository.findById(id)

    suspend fun getAllActive(): List<Category> = repository.findAllActive()

    suspend fun update(id: Int, category: NewCategory) = repository.update(id, category)

    suspend fun delete(id: Int) = repository.softDelete(id)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryServiceTest"`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/categories/CategoryService.kt src/test/kotlin/features/categories/CategoryServiceTest.kt
git commit -m "feat: add CategoryService with CRUD and soft delete"
```

---

## Task 5: Category Routes, Module, and Wiring

**Files:**
- Create: `src/main/kotlin/features/categories/CategoryRoutes.kt`
- Create: `src/main/kotlin/features/categories/CategoryModule.kt`
- Create: `src/test/kotlin/features/categories/CategoryRoutesTest.kt`
- Modify: `src/main/kotlin/Frameworks.kt`
- Modify: `src/main/kotlin/Routing.kt`

- [ ] **Step 1: Write failing route tests**

Create `src/test/kotlin/features/categories/CategoryRoutesTest.kt`:

```kotlin
package dev.jcasas.features.categories

import dev.jcasas.configureSerialization
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CategoryRoutesTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Categories)
            SchemaUtils.create(Categories)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureCategoryRoutes(CategoryService(CategoryRepository()))
            }
            block()
        }

    @Test
    fun `POST categories creates and returns 201`() = withApp {
        val response = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET categories returns all active categories`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }

        val response = client.get("/categories")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Food")
    }

    @Test
    fun `GET categories by id returns the category`() = withApp {
        val created = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.get("/categories/$id")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Food")
    }

    @Test
    fun `GET categories by id returns 404 when not found`() = withApp {
        val response = client.get("/categories/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT categories updates and returns 200`() = withApp {
        val created = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.put("/categories/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Groceries","type":"EXPENSE","defaultAllocationCents":60000}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE categories soft-deletes and returns 200`() = withApp {
        val created = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.delete("/categories/$id")

        assertEquals(HttpStatusCode.OK, response.status)

        // Category still exists but is not in active list
        val allResponse = client.get("/categories")
        val body = allResponse.bodyAsText()
        assertEquals("[]", body)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRoutesTest"`
Expected: FAIL — `configureCategoryRoutes` not found

- [ ] **Step 3: Implement CategoryRoutes.kt**

Create `src/main/kotlin/features/categories/CategoryRoutes.kt`:

```kotlin
package dev.jcasas.features.categories

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureCategoryRoutes(service: CategoryService) {
    routing {
        post("/categories") {
            val request = call.receive<CategoryRequest>()
            val id = service.create(request.toNewCategory())
            call.respond(HttpStatusCode.Created, id)
        }

        get("/categories") {
            val categories = service.getAllActive().map { CategoryResponse.from(it) }
            call.respond(HttpStatusCode.OK, categories)
        }

        get("/categories/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val category = service.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(HttpStatusCode.OK, CategoryResponse.from(category))
        }

        put("/categories/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CategoryRequest>()
            service.update(id, request.toNewCategory())
            call.respond(HttpStatusCode.OK)
        }

        delete("/categories/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

- [ ] **Step 4: Create CategoryModule.kt**

Create `src/main/kotlin/features/categories/CategoryModule.kt`:

```kotlin
package dev.jcasas.features.categories

import org.koin.dsl.module

val categoryModule = module {
    single { CategoryRepository() }
    single { CategoryService(get()) }
}
```

- [ ] **Step 5: Run route tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.categories.CategoryRoutesTest"`
Expected: All 6 tests PASS

- [ ] **Step 6: Wire into Frameworks.kt and Routing.kt**

In `src/main/kotlin/Frameworks.kt`, add import and module:

```kotlin
import dev.jcasas.features.categories.categoryModule
// ...
modules(transactionModule, categoryModule)
```

In `src/main/kotlin/Routing.kt`, add:

```kotlin
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
// ...
val categoryService: CategoryService by inject()
configureCategoryRoutes(categoryService)
```

- [ ] **Step 7: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS (existing + new category tests)

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/features/categories/CategoryRoutes.kt src/main/kotlin/features/categories/CategoryModule.kt src/test/kotlin/features/categories/CategoryRoutesTest.kt src/main/kotlin/Frameworks.kt src/main/kotlin/Routing.kt
git commit -m "feat: add Category routes, module, and wire into application"
```

---

## Task 6: Budget Models and Schema

**Files:**
- Create: `src/main/kotlin/features/budgets/BudgetModels.kt`
- Create: `src/main/kotlin/features/budgets/BudgetSchema.kt`

- [ ] **Step 1: Create BudgetModels.kt**

Create `src/main/kotlin/features/budgets/BudgetModels.kt`:

```kotlin
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
    val allocationCents: Long? = null,
    val snoozed: Boolean? = null,
)

@Serializable
data class BudgetItemResponse(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryType: TransactionType,
    val allocationCents: Long,
    val spentCents: Long,
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
```

- [ ] **Step 2: Create BudgetSchema.kt**

Create `src/main/kotlin/features/budgets/BudgetSchema.kt`:

```kotlin
package dev.jcasas.features.budgets

import dev.jcasas.features.categories.Categories
import org.jetbrains.exposed.sql.Table

object Budgets : Table() {
    val id = integer("id").autoIncrement()
    val year = integer("year")
    val month = integer("month")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(year, month)
    }
}

object BudgetItems : Table("budget_items") {
    val id = integer("id").autoIncrement()
    val budgetId = integer("budget_id").references(Budgets.id)
    val categoryId = integer("category_id").references(Categories.id)
    val allocationCents = long("allocation_cents")
    val snoozed = bool("snoozed").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(budgetId, categoryId)
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/features/budgets/
git commit -m "feat: add Budget models and schema"
```

---

## Task 7: Budget Repository

**Files:**
- Create: `src/main/kotlin/features/budgets/BudgetRepository.kt`
- Create: `src/test/kotlin/features/budgets/BudgetRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Create `src/test/kotlin/features/budgets/BudgetRepositoryTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetRepositoryTest"`
Expected: FAIL — `BudgetRepository` not found

- [ ] **Step 3: Implement BudgetRepository**

Create `src/main/kotlin/features/budgets/BudgetRepository.kt`:

```kotlin
package dev.jcasas.features.budgets

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class BudgetRepository {

    suspend fun createBudget(year: Int, month: Int): Int = newSuspendedTransaction(Dispatchers.IO) {
        Budgets.insert {
            it[Budgets.year] = year
            it[Budgets.month] = month
        }[Budgets.id]
    }

    suspend fun findBudgetByYearMonth(year: Int, month: Int): Budget? = newSuspendedTransaction(Dispatchers.IO) {
        Budgets.selectAll()
            .where { (Budgets.year eq year) and (Budgets.month eq month) }
            .map { row ->
                Budget(
                    id = row[Budgets.id],
                    year = row[Budgets.year],
                    month = row[Budgets.month],
                )
            }
            .singleOrNull()
    }

    suspend fun findAllBudgets(year: Int?): List<Budget> = newSuspendedTransaction(Dispatchers.IO) {
        val query = Budgets.selectAll()
        if (year != null) {
            query.where { Budgets.year eq year }
        }
        query.map { row ->
            Budget(
                id = row[Budgets.id],
                year = row[Budgets.year],
                month = row[Budgets.month],
            )
        }
    }

    /**
     * Creates a budget and all its items atomically in a single transaction.
     * Per the architecture spec's multi-step transaction pattern, this wraps
     * the multi-table write in one newSuspendedTransaction.
     */
    suspend fun createBudgetWithItems(
        year: Int,
        month: Int,
        items: List<Pair<Int, Long>>,
    ): Budget = newSuspendedTransaction(Dispatchers.IO) {
        val budgetId = Budgets.insert {
            it[Budgets.year] = year
            it[Budgets.month] = month
        }[Budgets.id]

        for ((categoryId, allocationCents) in items) {
            BudgetItems.insert {
                it[BudgetItems.budgetId] = budgetId
                it[BudgetItems.categoryId] = categoryId
                it[BudgetItems.allocationCents] = allocationCents
            }
        }

        Budget(id = budgetId, year = year, month = month)
    }

    suspend fun findBudgetItemById(id: Int): BudgetItem? = newSuspendedTransaction(Dispatchers.IO) {
        BudgetItems.selectAll()
            .where { BudgetItems.id eq id }
            .map { row ->
                BudgetItem(
                    id = row[BudgetItems.id],
                    budgetId = row[BudgetItems.budgetId],
                    categoryId = row[BudgetItems.categoryId],
                    allocationCents = row[BudgetItems.allocationCents],
                    snoozed = row[BudgetItems.snoozed],
                )
            }
            .singleOrNull()
    }

    suspend fun findBudgetItemByBudgetAndCategory(budgetId: Int, categoryId: Int): BudgetItem? =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.selectAll()
                .where { (BudgetItems.budgetId eq budgetId) and (BudgetItems.categoryId eq categoryId) }
                .map { row ->
                    BudgetItem(
                        id = row[BudgetItems.id],
                        budgetId = row[BudgetItems.budgetId],
                        categoryId = row[BudgetItems.categoryId],
                        allocationCents = row[BudgetItems.allocationCents],
                        snoozed = row[BudgetItems.snoozed],
                    )
                }
                .singleOrNull()
        }

    suspend fun findBudgetItemsByBudgetId(budgetId: Int): List<BudgetItem> =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.selectAll()
                .where { BudgetItems.budgetId eq budgetId }
                .map { row ->
                    BudgetItem(
                        id = row[BudgetItems.id],
                        budgetId = row[BudgetItems.budgetId],
                        categoryId = row[BudgetItems.categoryId],
                        allocationCents = row[BudgetItems.allocationCents],
                        snoozed = row[BudgetItems.snoozed],
                    )
                }
        }

    suspend fun updateBudgetItem(id: Int, allocationCents: Long?, snoozed: Boolean?) =
        newSuspendedTransaction(Dispatchers.IO) {
            BudgetItems.update({ BudgetItems.id eq id }) {
                if (allocationCents != null) it[BudgetItems.allocationCents] = allocationCents
                if (snoozed != null) it[BudgetItems.snoozed] = snoozed
            }
        }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetRepositoryTest"`
Expected: All 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/budgets/BudgetRepository.kt src/test/kotlin/features/budgets/BudgetRepositoryTest.kt
git commit -m "feat: add BudgetRepository with budget and budget item CRUD"
```

---

## Task 8: Budget Service with Auto-Creation

**Files:**
- Create: `src/main/kotlin/features/budgets/BudgetService.kt`
- Create: `src/test/kotlin/features/budgets/BudgetServiceTest.kt`

The BudgetService depends on CategoryService (to get active categories for auto-creation).

- [ ] **Step 1: Write failing service tests**

Create `src/test/kotlin/features/budgets/BudgetServiceTest.kt`:

```kotlin
package dev.jcasas.features.budgets

import dev.jcasas.TransactionType
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.NewCategory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems)
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

        assertEquals(50000L, response.items[0].allocationCents)
    }

    @Test
    fun `getOrCreateBudget returns spent totals as zero when no transactions`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))

        val response = budgetService.getOrCreateBudget(2026, 3)

        assertEquals(0L, response.items[0].spentCents)
    }

    @Test
    fun `updateBudgetItem changes allocation`() = runBlocking {
        categoryService.create(NewCategory("Food", TransactionType.EXPENSE, 50000))
        val budget = budgetService.getOrCreateBudget(2026, 3)
        val itemId = budget.items[0].id

        budgetService.updateBudgetItem(itemId, allocationCents = 60000, snoozed = null)

        val updated = budgetService.getOrCreateBudget(2026, 3)
        assertEquals(60000L, updated.items[0].allocationCents)
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetServiceTest"`
Expected: FAIL — `BudgetService` not found

- [ ] **Step 3: Implement BudgetService**

Create `src/main/kotlin/features/budgets/BudgetService.kt`:

```kotlin
package dev.jcasas.features.budgets

import dev.jcasas.features.categories.CategoryService

class BudgetService(
    private val repository: BudgetRepository,
    private val categoryService: CategoryService,
) {

    suspend fun getOrCreateBudget(year: Int, month: Int): BudgetResponse {
        val budget = repository.findBudgetByYearMonth(year, month)
            ?: autoCreateBudget(year, month)

        val items = repository.findBudgetItemsByBudgetId(budget.id)
        val spentByItem = getSpentByBudgetItems(items.map { it.id })

        val itemResponses = items.map { item ->
            val category = categoryService.getById(item.categoryId)!!
            BudgetItemResponse(
                id = item.id,
                categoryId = item.categoryId,
                categoryName = category.name,
                categoryType = category.type,
                allocationCents = item.allocationCents,
                spentCents = spentByItem[item.id] ?: 0L,
                snoozed = item.snoozed,
            )
        }

        return BudgetResponse(
            id = budget.id,
            year = budget.year,
            month = budget.month,
            items = itemResponses,
        )
    }

    private suspend fun autoCreateBudget(year: Int, month: Int): Budget {
        val activeCategories = categoryService.getAllActive()
        val items = activeCategories.map { it.id to it.defaultAllocationCents }
        return repository.createBudgetWithItems(year, month, items)
    }

    suspend fun findBudgetItemForTransaction(categoryId: Int, year: Int, month: Int): BudgetItem? {
        val budget = repository.findBudgetByYearMonth(year, month)
            ?: autoCreateBudget(year, month)

        return repository.findBudgetItemByBudgetAndCategory(budget.id, categoryId)
    }

    suspend fun updateBudgetItem(itemId: Int, allocationCents: Long?, snoozed: Boolean?) {
        repository.updateBudgetItem(itemId, allocationCents, snoozed)
    }

    suspend fun getAllBudgets(year: Int?): List<BudgetSummaryResponse> {
        return repository.findAllBudgets(year).map { budget ->
            BudgetSummaryResponse(
                id = budget.id,
                year = budget.year,
                month = budget.month,
            )
        }
    }

    private suspend fun getSpentByBudgetItems(itemIds: List<Int>): Map<Int, Long> {
        // Placeholder: returns empty until Transactions schema adds budgetItemId (Task 13)
        return emptyMap()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetServiceTest"`
Expected: All 11 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/budgets/BudgetService.kt src/test/kotlin/features/budgets/BudgetServiceTest.kt
git commit -m "feat: add BudgetService with auto-creation logic"
```

---

## Task 9: Budget Routes, Module, and Wiring

**Files:**
- Create: `src/main/kotlin/features/budgets/BudgetRoutes.kt`
- Create: `src/main/kotlin/features/budgets/BudgetModule.kt`
- Create: `src/test/kotlin/features/budgets/BudgetRoutesTest.kt`
- Modify: `src/main/kotlin/Frameworks.kt`
- Modify: `src/main/kotlin/Routing.kt`

- [ ] **Step 1: Write failing route tests**

Create `src/test/kotlin/features/budgets/BudgetRoutesTest.kt`:

```kotlin
package dev.jcasas.features.budgets

import dev.jcasas.configureSerialization
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryRoutes
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class BudgetRoutesTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            val categoryService = CategoryService(CategoryRepository())
            val budgetService = BudgetService(BudgetRepository(), categoryService)
            application {
                configureSerialization()
                configureCategoryRoutes(categoryService)
                configureBudgetRoutes(budgetService)
            }
            block()
        }

    @Test
    fun `GET budgets year month auto-creates and returns budget`() = withApp {
        // Create a category first
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }

        val response = client.get("/budgets/2026/3")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "Food")
        assertContains(body, "50000")
    }

    @Test
    fun `GET budgets year month returns 400 for invalid year or month`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/budgets/abc/3").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/budgets/2026/abc").status)
    }

    @Test
    fun `GET budgets returns list of all budgets`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        client.get("/budgets/2026/1")
        client.get("/budgets/2026/2")

        val response = client.get("/budgets")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "2026")
    }

    @Test
    fun `GET budgets filters by year query param`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        client.get("/budgets/2026/1")
        client.get("/budgets/2025/12")

        val response = client.get("/budgets?year=2026")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "2026")
    }

    @Test
    fun `PUT budget item updates allocation`() = withApp {
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        val budgetResponse = client.get("/budgets/2026/3")
        val body = budgetResponse.bodyAsText()
        // Extract item id from response — items array contains objects with "id" field
        // Match the id inside the "items" array (not the top-level budget id)
        val itemIdMatch = Regex(""""items":\[.*?"id":(\d+)""").find(body)!!
        val itemId = itemIdMatch.groupValues[1]

        val response = client.put("/budgets/2026/3/items/$itemId") {
            contentType(ContentType.Application.Json)
            setBody("""{"allocationCents":60000}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetRoutesTest"`
Expected: FAIL — `configureBudgetRoutes` not found

- [ ] **Step 3: Implement BudgetRoutes.kt**

Create `src/main/kotlin/features/budgets/BudgetRoutes.kt`:

```kotlin
package dev.jcasas.features.budgets

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureBudgetRoutes(service: BudgetService) {
    routing {
        get("/budgets") {
            val year = call.parameters["year"]?.toIntOrNull()
            val budgets = service.getAllBudgets(year)
            call.respond(HttpStatusCode.OK, budgets)
        }

        get("/budgets/{year}/{month}") {
            val year = call.parameters["year"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val month = call.parameters["month"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val budget = service.getOrCreateBudget(year, month)
            call.respond(HttpStatusCode.OK, budget)
        }

        put("/budgets/{year}/{month}/items/{itemId}") {
            val itemId = call.parameters["itemId"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<BudgetItemUpdateRequest>()
            service.updateBudgetItem(itemId, request.allocationCents, request.snoozed)
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

- [ ] **Step 4: Create BudgetModule.kt**

Create `src/main/kotlin/features/budgets/BudgetModule.kt`:

```kotlin
package dev.jcasas.features.budgets

import org.koin.dsl.module

val budgetModule = module {
    single { BudgetRepository() }
    single { BudgetService(get(), get()) }
}
```

- [ ] **Step 5: Run route tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetRoutesTest"`
Expected: All 5 tests PASS

- [ ] **Step 6: Wire into Frameworks.kt and Routing.kt**

In `src/main/kotlin/Frameworks.kt`, add:

```kotlin
import dev.jcasas.features.budgets.budgetModule
// ...
modules(transactionModule, categoryModule, budgetModule)
```

In `src/main/kotlin/Routing.kt`, add:

```kotlin
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.budgets.configureBudgetRoutes
// ...
val budgetService: BudgetService by inject()
configureBudgetRoutes(budgetService)
```

- [ ] **Step 7: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/features/budgets/BudgetRoutes.kt src/main/kotlin/features/budgets/BudgetModule.kt src/test/kotlin/features/budgets/BudgetRoutesTest.kt src/main/kotlin/Frameworks.kt src/main/kotlin/Routing.kt
git commit -m "feat: add Budget routes, module, and wire into application"
```

---

## Task 10: Modify Transaction Schema, Models, and Repository

**Files:**
- Modify: `src/main/kotlin/features/transactions/TransactionSchema.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionModels.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionRepository.kt`
- Modify: `src/test/kotlin/features/transactions/TransactionRepositoryTest.kt`

**Note:** Schema, models, and repository are updated together to keep the project in a compilable state.

- [ ] **Step 1: Add budgetItemId FK to TransactionSchema.kt**

In `src/main/kotlin/features/transactions/TransactionSchema.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Transactions : Table() {
    val id = integer("id").autoIncrement()
    val amountCents = long("amount_cents")
    val type = enumerationByName<TransactionType>("type", 50)
    val description = varchar("description", 255)
    val date = date("date")
    val budgetItemId = integer("budget_item_id").references(BudgetItems.id)

    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 2: Update TransactionModels.kt**

Replace the full file content:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val id: Int,
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
    val budgetItemId: Int,
    val categoryId: Int,
)

data class NewTransaction(
    val amount: BigDecimal,
    val categoryId: Int,
    val description: String,
    val date: LocalDate,
)

@Serializable
data class TransactionRequest(
    val amount: String,
    val categoryId: Int,
    val description: String,
    val date: String,
) {
    fun toNewTransaction() = NewTransaction(
        amount = BigDecimal(amount),
        categoryId = categoryId,
        description = description,
        date = LocalDate.parse(date),
    )
}

@Serializable
data class TransactionResponse(
    val id: Int,
    val amount: String,
    val type: TransactionType,
    val description: String,
    val date: String,
    val budgetItemId: Int,
    val categoryId: Int,
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            id = transaction.id,
            amount = transaction.amount.toPlainString(),
            type = transaction.type,
            description = transaction.description,
            date = transaction.date.toString(),
            budgetItemId = transaction.budgetItemId,
            categoryId = transaction.categoryId,
        )
    }
}
```

- [ ] **Step 3: Update repository tests for new schema**

Replace `src/test/kotlin/features/transactions/TransactionRepositoryTest.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionRepositoryTest {

    private lateinit var repository: TransactionRepository
    private var budgetItemId: Int = 0
    private var categoryId: Int = 0

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)

            // Create prerequisite data
            categoryId = Categories.insert {
                it[name] = "Food"
                it[type] = TransactionType.EXPENSE
                it[defaultAllocationCents] = 50000
            }[Categories.id]

            val budgetId = Budgets.insert {
                it[year] = 2026
                it[month] = 3
            }[Budgets.id]

            budgetItemId = BudgetItems.insert {
                it[BudgetItems.budgetId] = budgetId
                it[BudgetItems.categoryId] = categoryId
                it[allocationCents] = 50000
            }[BudgetItems.id]
        }
        repository = TransactionRepository()
    }

    @Test
    fun `create stores a transaction and returns its id`() = runBlocking {
        val id = repository.create(
            NewTransaction(
                amount = BigDecimal("25.50"),
                categoryId = categoryId,
                description = "Grocery shopping",
                date = LocalDate.of(2026, 3, 20),
            ),
            budgetItemId = budgetItemId,
            type = TransactionType.EXPENSE,
        )

        assertTrue(id > 0)
    }

    @Test
    fun `findById returns transaction with correct fields`() = runBlocking {
        val id = repository.create(
            NewTransaction(
                amount = BigDecimal("25.50"),
                categoryId = categoryId,
                description = "Grocery shopping",
                date = LocalDate.of(2026, 3, 20),
            ),
            budgetItemId = budgetItemId,
            type = TransactionType.EXPENSE,
        )

        val found = repository.findById(id)

        assertNotNull(found)
        assertEquals(id, found.id)
        assertEquals(BigDecimal("25.50"), found.amount)
        assertEquals(TransactionType.EXPENSE, found.type)
        assertEquals("Grocery shopping", found.description)
        assertEquals(LocalDate.of(2026, 3, 20), found.date)
        assertEquals(budgetItemId, found.budgetItemId)
        assertEquals(categoryId, found.categoryId)
    }

    @Test
    fun `findById returns null when transaction does not exist`() = runBlocking {
        assertNull(repository.findById(999))
    }

    @Test
    fun `findAll returns all stored transactions`() = runBlocking {
        repository.create(
            NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
            budgetItemId, TransactionType.EXPENSE,
        )
        repository.create(
            NewTransaction(BigDecimal("15.00"), categoryId, "Lunch", LocalDate.of(2026, 3, 20)),
            budgetItemId, TransactionType.EXPENSE,
        )

        val all = repository.findAll()

        assertEquals(2, all.size)
    }

    @Test
    fun `update changes the transaction fields`() = runBlocking {
        val id = repository.create(
            NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
            budgetItemId, TransactionType.EXPENSE,
        )

        repository.update(
            id,
            NewTransaction(BigDecimal("12.50"), categoryId, "Coffee and cake", LocalDate.of(2026, 3, 21)),
            budgetItemId, TransactionType.EXPENSE,
        )

        val updated = repository.findById(id)
        assertNotNull(updated)
        assertEquals(BigDecimal("12.50"), updated.amount)
        assertEquals("Coffee and cake", updated.description)
    }

    @Test
    fun `delete removes the transaction`() = runBlocking {
        val id = repository.create(
            NewTransaction(BigDecimal("10.00"), categoryId, "Coffee", LocalDate.of(2026, 3, 20)),
            budgetItemId, TransactionType.EXPENSE,
        )

        repository.delete(id)

        assertNull(repository.findById(id))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionRepositoryTest"`
Expected: FAIL — repository method signatures don't match

- [ ] **Step 3: Update TransactionRepository.kt**

Replace `src/main/kotlin/features/transactions/TransactionRepository.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class TransactionRepository {

    suspend fun create(transaction: NewTransaction, budgetItemId: Int, type: TransactionType): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.insert {
                it[amountCents] = transaction.amount.movePointRight(2).toLong()
                it[Transactions.type] = type
                it[description] = transaction.description
                it[date] = transaction.date
                it[Transactions.budgetItemId] = budgetItemId
            }[Transactions.id]
        }

    suspend fun findById(id: Int): Transaction? = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.join(BudgetItems, JoinType.INNER, Transactions.budgetItemId, BudgetItems.id)
            .selectAll()
            .where { Transactions.id eq id }
            .map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                    budgetItemId = row[Transactions.budgetItemId],
                    categoryId = row[BudgetItems.categoryId],
                )
            }
            .singleOrNull()
    }

    suspend fun findAll(categoryId: Int? = null, year: Int? = null, month: Int? = null): List<Transaction> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = Transactions
                .join(BudgetItems, JoinType.INNER, Transactions.budgetItemId, BudgetItems.id)
                .join(Budgets, JoinType.INNER, BudgetItems.budgetId, Budgets.id)
                .selectAll()

            if (categoryId != null) {
                query.andWhere { BudgetItems.categoryId eq categoryId }
            }
            if (year != null) {
                query.andWhere { Budgets.year eq year }
            }
            if (month != null) {
                query.andWhere { Budgets.month eq month }
            }

            query.map { row ->
                Transaction(
                    id = row[Transactions.id],
                    amount = BigDecimal(row[Transactions.amountCents]).movePointLeft(2),
                    type = row[Transactions.type],
                    description = row[Transactions.description],
                    date = row[Transactions.date],
                    budgetItemId = row[Transactions.budgetItemId],
                    categoryId = row[BudgetItems.categoryId],
                )
            }
        }

    suspend fun update(id: Int, transaction: NewTransaction, budgetItemId: Int, type: TransactionType) =
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.update({ Transactions.id eq id }) {
                it[amountCents] = transaction.amount.movePointRight(2).toLong()
                it[Transactions.type] = type
                it[description] = transaction.description
                it[date] = transaction.date
                it[Transactions.budgetItemId] = budgetItemId
            }
        }

    suspend fun delete(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Transactions.deleteWhere { Transactions.id eq id }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionRepositoryTest"`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/transactions/TransactionRepository.kt src/test/kotlin/features/transactions/TransactionRepositoryTest.kt
git commit -m "feat: update TransactionRepository for budget item linking"
```

---

## Task 11: Update Transaction Service with Budget Item Resolution

**Files:**
- Modify: `src/main/kotlin/features/transactions/TransactionService.kt`
- Modify: `src/main/kotlin/features/transactions/TransactionModule.kt`
- Modify: `src/test/kotlin/features/transactions/TransactionServiceTest.kt`

- [ ] **Step 1: Update service tests**

Replace `src/test/kotlin/features/transactions/TransactionServiceTest.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.TransactionType
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.BudgetRepository
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.NewCategory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionServiceTest {

    private lateinit var service: TransactionService
    private lateinit var categoryService: CategoryService
    private var foodCategoryId: Int = 0

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
        categoryService = CategoryService(CategoryRepository())
        val budgetService = BudgetService(BudgetRepository(), categoryService)
        service = TransactionService(TransactionRepository(), budgetService, categoryService)

        runBlocking {
            foodCategoryId = categoryService.create(
                NewCategory("Food", TransactionType.EXPENSE, 50000),
            )
        }
    }

    @Test
    fun `create resolves budget item and returns id`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 3, 20)),
        )

        assertTrue(id > 0)
    }

    @Test
    fun `create auto-creates budget when none exists`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 6, 15)),
        )

        val transaction = service.getById(id)
        assertNotNull(transaction)
        assertEquals(foodCategoryId, transaction.categoryId)
    }

    @Test
    fun `create infers type from category`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("25.50"), foodCategoryId, "Groceries", LocalDate.of(2026, 3, 20)),
        )

        val transaction = service.getById(id)
        assertNotNull(transaction)
        assertEquals(TransactionType.EXPENSE, transaction.type)
    }

    @Test
    fun `getById returns null when not found`() = runBlocking {
        assertNull(service.getById(999))
    }

    @Test
    fun `getAll returns all transactions`() = runBlocking {
        service.create(NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)))
        service.create(NewTransaction(BigDecimal("20.00"), foodCategoryId, "Lunch", LocalDate.of(2026, 3, 20)))

        val all = service.getAll()

        assertEquals(2, all.size)
    }

    @Test
    fun `update re-resolves budget item when date changes month`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        service.update(id, NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 4, 1)))

        val updated = service.getById(id)
        assertNotNull(updated)
        assertEquals(LocalDate.of(2026, 4, 1), updated.date)
    }

    @Test
    fun `delete removes the transaction`() = runBlocking {
        val id = service.create(
            NewTransaction(BigDecimal("10.00"), foodCategoryId, "Coffee", LocalDate.of(2026, 3, 20)),
        )

        service.delete(id)

        assertNull(service.getById(id))
    }

    @Test
    fun `create rejects transaction for soft-deleted category without budget item`() = runBlocking {
        val oldCategoryId = categoryService.create(
            NewCategory("OldStuff", TransactionType.EXPENSE, 10000),
        )
        categoryService.delete(oldCategoryId)

        var threw = false
        try {
            service.create(
                NewTransaction(BigDecimal("10.00"), oldCategoryId, "Test", LocalDate.of(2026, 5, 1)),
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }

        assertTrue(threw)
    }

    @Test
    fun `create allows transaction for soft-deleted category with existing budget item`() = runBlocking {
        val oldCategoryId = categoryService.create(
            NewCategory("OldStuff", TransactionType.EXPENSE, 10000),
        )
        // Create budget while category is still active — this creates its budget item
        val budgetService = BudgetService(BudgetRepository(), categoryService)
        budgetService.getOrCreateBudget(2026, 5)
        // Now soft-delete the category
        categoryService.delete(oldCategoryId)

        val id = service.create(
            NewTransaction(BigDecimal("10.00"), oldCategoryId, "Test", LocalDate.of(2026, 5, 1)),
        )

        assertTrue(id > 0)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionServiceTest"`
Expected: FAIL — constructor signature mismatch

- [ ] **Step 3: Update TransactionService.kt**

Replace `src/main/kotlin/features/transactions/TransactionService.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.categories.CategoryService

class TransactionService(
    private val repository: TransactionRepository,
    private val budgetService: BudgetService,
    private val categoryService: CategoryService,
) {

    suspend fun create(transaction: NewTransaction): Int {
        val category = categoryService.getById(transaction.categoryId)
            ?: throw IllegalArgumentException("Category ${transaction.categoryId} not found")

        val year = transaction.date.year
        val month = transaction.date.monthValue

        val budgetItem = budgetService.findBudgetItemForTransaction(transaction.categoryId, year, month)
            ?: throw IllegalArgumentException("Category ${transaction.categoryId} is not available for month $year-$month")

        return repository.create(transaction, budgetItem.id, category.type)
    }

    suspend fun getById(id: Int): Transaction? = repository.findById(id)

    suspend fun getAll(categoryId: Int? = null, year: Int? = null, month: Int? = null): List<Transaction> =
        repository.findAll(categoryId, year, month)

    suspend fun update(id: Int, transaction: NewTransaction) {
        val category = categoryService.getById(transaction.categoryId)
            ?: throw IllegalArgumentException("Category ${transaction.categoryId} not found")

        val year = transaction.date.year
        val month = transaction.date.monthValue

        val budgetItem = budgetService.findBudgetItemForTransaction(transaction.categoryId, year, month)
            ?: throw IllegalArgumentException("Category ${transaction.categoryId} is not available for month $year-$month")

        repository.update(id, transaction, budgetItem.id, category.type)
    }

    suspend fun delete(id: Int) = repository.delete(id)
}
```

- [ ] **Step 4: Update TransactionModule.kt**

Replace `src/main/kotlin/features/transactions/TransactionModule.kt`:

```kotlin
package dev.jcasas.features.transactions

import org.koin.dsl.module

val transactionModule = module {
    single { TransactionRepository() }
    single { TransactionService(get(), get(), get()) }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionServiceTest"`
Expected: All 9 tests PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/features/transactions/TransactionService.kt src/main/kotlin/features/transactions/TransactionModule.kt src/test/kotlin/features/transactions/TransactionServiceTest.kt
git commit -m "feat: update TransactionService with budget item resolution"
```

---

## Task 12: Update Transaction Routes and Route Tests

**Files:**
- Modify: `src/main/kotlin/features/transactions/TransactionRoutes.kt`
- Modify: `src/test/kotlin/features/transactions/TransactionRoutesTest.kt`

- [ ] **Step 1: Update route tests**

Replace `src/test/kotlin/features/transactions/TransactionRoutesTest.kt`:

```kotlin
package dev.jcasas.features.transactions

import dev.jcasas.configureSerialization
import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.BudgetRepository
import dev.jcasas.features.budgets.BudgetService
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.categories.CategoryRepository
import dev.jcasas.features.categories.CategoryService
import dev.jcasas.features.categories.configureCategoryRoutes
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TransactionRoutesTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.drop(Transactions, BudgetItems, Budgets, Categories)
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            val categoryService = CategoryService(CategoryRepository())
            val budgetService = BudgetService(BudgetRepository(), categoryService)
            val transactionService = TransactionService(TransactionRepository(), budgetService, categoryService)
            application {
                configureSerialization()
                configureCategoryRoutes(categoryService)
                configureTransactionRoutes(transactionService)
            }
            block()
        }

    private suspend fun ApplicationTestBuilder.createCategory(): String {
        val response = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Food","type":"EXPENSE","defaultAllocationCents":50000}""")
        }
        return response.bodyAsText().trim()
    }

    @Test
    fun `POST transactions creates and returns 201`() = withApp {
        val categoryId = createCategory()

        val response = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET transactions returns all transactions`() = withApp {
        val categoryId = createCategory()
        client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }

        val response = client.get("/transactions")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Groceries")
        assertContains(response.bodyAsText(), "budgetItemId")
        assertContains(response.bodyAsText(), "categoryId")
    }

    @Test
    fun `GET transactions by id returns the transaction`() = withApp {
        val categoryId = createCategory()
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.get("/transactions/$id")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Groceries")
    }

    @Test
    fun `GET transactions by id returns 404 when not found`() = withApp {
        val response = client.get("/transactions/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT transactions updates and returns 200`() = withApp {
        val categoryId = createCategory()
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.put("/transactions/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"30.00","categoryId":$categoryId,"description":"Groceries and drinks","date":"2026-03-20"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE transactions removes and returns 200`() = withApp {
        val categoryId = createCategory()
        val created = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }
        val id = created.bodyAsText().trim()

        val response = client.delete("/transactions/$id")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/transactions/$id").status)
    }

    @Test
    fun `GET transactions supports query filters`() = withApp {
        val categoryId = createCategory()
        client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"25.50","categoryId":$categoryId,"description":"Groceries","date":"2026-03-20"}""")
        }

        val response = client.get("/transactions?categoryId=$categoryId&year=2026&month=3")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Groceries")
    }

    @Test
    fun `POST transactions returns 422 for soft-deleted category without budget item`() = withApp {
        val categoryId = createCategory()
        // Soft-delete the category before any budget is created for the target month
        client.delete("/categories/$categoryId")

        val response = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":"10.00","categoryId":$categoryId,"description":"Test","date":"2026-05-01"}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionRoutesTest"`
Expected: FAIL — route handler code uses old model shapes

- [ ] **Step 3: Update TransactionRoutes.kt**

Replace `src/main/kotlin/features/transactions/TransactionRoutes.kt`:

```kotlin
package dev.jcasas.features.transactions

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureTransactionRoutes(service: TransactionService) {
    routing {
        post("/transactions") {
            val request = call.receive<TransactionRequest>()
            try {
                val id = service.create(request.toNewTransaction())
                call.respond(HttpStatusCode.Created, id)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to e.message))
            }
        }

        get("/transactions") {
            val categoryId = call.parameters["categoryId"]?.toIntOrNull()
            val year = call.parameters["year"]?.toIntOrNull()
            val month = call.parameters["month"]?.toIntOrNull()
            val transactions = service.getAll(categoryId, year, month).map { TransactionResponse.from(it) }
            call.respond(HttpStatusCode.OK, transactions)
        }

        get("/transactions/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val transaction = service.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(HttpStatusCode.OK, TransactionResponse.from(transaction))
        }

        put("/transactions/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<TransactionRequest>()
            try {
                service.update(id, request.toNewTransaction())
                call.respond(HttpStatusCode.OK)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to e.message))
            }
        }

        delete("/transactions/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

- [ ] **Step 4: Run route tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.transactions.TransactionRoutesTest"`
Expected: All 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/features/transactions/TransactionRoutes.kt src/test/kotlin/features/transactions/TransactionRoutesTest.kt
git commit -m "feat: update Transaction routes for budget-linked requests"
```

---

## Task 13: Implement Spent Totals in BudgetService

**Files:**
- Modify: `src/main/kotlin/features/budgets/BudgetService.kt`
- Create: `src/test/kotlin/features/budgets/BudgetSpentTotalsTest.kt`

Now that Transactions schema has `budgetItemId`, we can implement the real spent totals query.

- [ ] **Step 1: Write failing spent totals test**

Create `src/test/kotlin/features/budgets/BudgetSpentTotalsTest.kt`:

```kotlin
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
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetSpentTotalsTest"`
Expected: The first test should FAIL (spentCents = 0 instead of 3550) because `getSpentByBudgetItems` currently returns an empty map.

- [ ] **Step 3: Implement real spent totals query in BudgetService**

In `src/main/kotlin/features/budgets/BudgetService.kt`, replace the placeholder `getSpentByBudgetItems` method:

```kotlin
    private suspend fun getSpentByBudgetItems(itemIds: List<Int>): Map<Int, Long> {
        if (itemIds.isEmpty()) return emptyMap()
        return newSuspendedTransaction(Dispatchers.IO) {
            val sumCol = Transactions.amountCents.sum()
            Transactions.select(Transactions.budgetItemId, sumCol)
                .where { Transactions.budgetItemId inList itemIds }
                .groupBy(Transactions.budgetItemId)
                .associate { row ->
                    row[Transactions.budgetItemId] to (row[sumCol] ?: 0L)
                }
        }
    }
```

Add the required imports to BudgetService.kt:

```kotlin
import dev.jcasas.features.transactions.Transactions
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.jcasas.features.budgets.BudgetSpentTotalsTest"`
Expected: All 2 tests PASS

- [ ] **Step 5: Run all tests**

Run: `./gradlew test`
Expected: ALL tests PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/features/budgets/BudgetService.kt src/test/kotlin/features/budgets/BudgetSpentTotalsTest.kt
git commit -m "feat: implement spent totals query in BudgetService"
```

---

## Task 14: Final Verification

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: All tests PASS (approximately 50+ tests across all features)

- [ ] **Step 2: Verify compilation with no warnings**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify application starts**

Run: `./gradlew run` (stop after startup — requires DB config)
Or verify via: `./gradlew build`
Expected: BUILD SUCCESSFUL

---

## Summary

| Task | Feature | What |
|---|---|---|
| 1 | Shared | Extract TransactionType to Models.kt |
| 2 | Categories | Models + Schema |
| 3 | Categories | Repository + tests |
| 4 | Categories | Service + tests |
| 5 | Categories | Routes + Module + wiring + tests |
| 6 | Budgets | Models + Schema |
| 7 | Budgets | Repository + tests |
| 8 | Budgets | Service (auto-creation) + tests |
| 9 | Budgets | Routes + Module + wiring + tests |
| 10 | Transactions | Schema + Models + Repository update + tests |
| 11 | Transactions | Service update (budget resolution) + tests |
| 12 | Transactions | Routes update + tests |
| 13 | Budgets | Spent totals implementation + tests |
| 14 | All | Final verification |
