# 12. Tutorial: add a `categories` feature

In this tutorial you build a complete feature yourself. When you are done, you have touched every layer and every
place that needs wiring, and you can do the same for any feature you invent.

> The code in this chapter has been compiled and tested against the project. If yours does not compile, compare it
> line by line with the code here.

## What we build

Categories such as "Bugs", "Billing" or "Feature request". Later, other features (for example feedback) can point to a
category.

| Endpoint | Access | Answer |
|---|---|---|
| `GET /api/v1/categories?q=&sort=&order=&limit=&offset=` | every logged-in user | 200, a page |
| `GET /api/v1/categories/{id}` | every logged-in user | 200, or 404 |
| `POST /api/v1/categories` | `category.manage` | 201, 400, 403, 409 |
| `PATCH /api/v1/categories/{id}` | `category.manage` | 200, 400, 403, 404, 409 |
| `DELETE /api/v1/categories/{id}` | `category.manage` | 204, 403, 404 |

How it differs from `feedback`, so you learn something new:

| | `feedback` | `categories` |
|---|---|---|
| Who owns it? | a user | nobody: global |
| Who may change it? | the owner, or a permission | only with a permission |
| Unique field | none | `name`: a duplicate gives **409** |
| Default sort | newest first | alphabetical, A to Z |

## Step 0: the plan

Write down the files before you start. You will create (all under `backend/src/features/category/`):

```text
model/       Category.kt, CategorySortField.kt, CategoryQuery.kt
database/    CategoriesTable.kt
dto/         CategoryRequest.kt, CategoryResponse.kt
mapper/      CategoryMapper.kt, CategoryDatabaseMapper.kt, CategoryQueryMapper.kt
repository/  CategoryRepository.kt, ExposedCategoryRepository.kt
service/     CategoryService.kt
route/       CategoryRoutes.kt
```

and change: `Permission.kt`, `DatabaseFactory.kt`, `Application.kt`, `Routing.kt`, `TestFixtures.kt`.

## Step 1: the model

A `Category` is what the application works with. Not serializable: it never goes to the client directly.

```kotlin
// model/Category.kt
package nl.brul.backend.features.category.model

import java.time.Instant
import java.util.UUID

data class Category(
    val id: UUID,
    val name: String,
    val createdAt: Instant
)
```

## Step 2: the table

```kotlin
// database/CategoriesTable.kt
package nl.brul.backend.features.category.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CategoriesTable : Table("categories") {
    val id = uuid("id")
    /** Unique: the database itself refuses a second category with the same name. */
    val name = varchar("name", 60).uniqueIndex()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

Register it in `backend/src/config/DatabaseFactory.kt`:

```kotlin
SchemaUtils.create(
    UsersTable,
    RolesTable,
    PermissionsTable,
    UserRolesTable,
    RolePermissionsTable,
    FeedbackTable,
    CategoriesTable          // new
)
```

Because it is a **new** table, you do not have to delete your local database (chapter 9).

## Step 3: the DTOs

What the client sends and receives. A category only has a name to change, so create and rename share one request.

```kotlin
// dto/CategoryRequest.kt
package nl.brul.backend.features.category.dto

import kotlinx.serialization.Serializable

/** Body of both `POST /categories` and `PATCH /categories/{id}`: a category only has a name to change. */
@Serializable
data class CategoryRequest(
    val name: String
)
```

```kotlin
// dto/CategoryResponse.kt
package nl.brul.backend.features.category.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    /** ISO 8601 in UTC. */
    val createdAt: String
)
```

Note: ids and timestamps are `String` in the DTO. The mapper converts them.

## Step 4: the mappers

```kotlin
// mapper/CategoryMapper.kt
package nl.brul.backend.features.category.mapper

import nl.brul.backend.features.category.dto.CategoryResponse
import nl.brul.backend.features.category.model.Category

fun Category.toResponse(): CategoryResponse =
    CategoryResponse(
        id = id.toString(),
        name = name,
        createdAt = createdAt.toString()
    )
```

```kotlin
// mapper/CategoryDatabaseMapper.kt
package nl.brul.backend.features.category.mapper

import nl.brul.backend.features.category.database.CategoriesTable
import nl.brul.backend.features.category.model.Category
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCategory(): Category =
    Category(
        id = this[CategoriesTable.id],
        name = this[CategoriesTable.name],
        createdAt = this[CategoriesTable.createdAt]
    )
```

## Step 5: search support

The list needs a query model, the sortable fields and a mapper from query parameters (chapter 10).

```kotlin
// model/CategorySortField.kt
package nl.brul.backend.features.category.model

/** Fields a category list can be sorted on; [key] is the value of the `sort` query parameter. */
enum class CategorySortField(val key: String) {
    NAME("name"),
    CREATED_AT("createdAt")
}
```

```kotlin
// model/CategoryQuery.kt
package nl.brul.backend.features.category.model

import nl.brul.backend.shared.paging.Page
import nl.brul.backend.shared.paging.SortDirection

/** What `GET /categories` asks for. The default is alphabetical by name. */
data class CategoryQuery(
    /** Matches part of the name, ignoring case. */
    val text: String? = null,
    val sortField: CategorySortField = CategorySortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
    val page: Page = Page()
)
```

```kotlin
// mapper/CategoryQueryMapper.kt
package nl.brul.backend.features.category.mapper

import io.ktor.http.Parameters
import nl.brul.backend.features.category.model.CategoryQuery
import nl.brul.backend.features.category.model.CategorySortField
import nl.brul.backend.shared.http.oneOf
import nl.brul.backend.shared.http.page
import nl.brul.backend.shared.http.searchText
import nl.brul.backend.shared.paging.SortDirection

/** Reads `?q=&sort=name|createdAt&order=asc|desc&limit=&offset=`; an invalid value gives 400. */
fun Parameters.toCategoryQuery(): CategoryQuery =
    CategoryQuery(
        text = searchText("q"),
        sortField = oneOf("sort", CategorySortField.entries.associateBy { it.key }) ?: CategorySortField.NAME,
        // An alphabetical list reads best from A to Z, so the default here is ascending.
        sortDirection = oneOf("order", mapOf("asc" to SortDirection.ASC, "desc" to SortDirection.DESC))
            ?: SortDirection.ASC,
        page = page()
    )
```

Why not `sortDirection()` from `QueryParameters.kt`? That helper defaults to `DESC` (newest first). For names, A to Z
is the better default, so we use `oneOf` directly with our own default.

## Step 6: the repository

First the interface: what the service may ask.

```kotlin
// repository/CategoryRepository.kt
package nl.brul.backend.features.category.repository

import nl.brul.backend.features.category.model.Category
import nl.brul.backend.features.category.model.CategoryQuery
import java.util.UUID

interface CategoryRepository {
    fun findById(id: UUID): Category?
    fun findByName(name: String): Category?
    fun search(query: CategoryQuery): List<Category>
    fun count(query: CategoryQuery): Long

    /** Throws 409 when the name is already taken. */
    fun save(category: Category): Category

    /** Throws 409 when the new name is already taken. */
    fun update(category: Category): Boolean

    fun delete(id: UUID): Boolean
}
```

Then the Exposed implementation. New compared with feedback: turning the unique-index error into a 409 (chapter 9).

```kotlin
// repository/ExposedCategoryRepository.kt
package nl.brul.backend.features.category.repository

import nl.brul.backend.features.category.database.CategoriesTable
import nl.brul.backend.features.category.mapper.toCategory
import nl.brul.backend.features.category.model.Category
import nl.brul.backend.features.category.model.CategoryQuery
import nl.brul.backend.features.category.model.CategorySortField
import nl.brul.backend.shared.database.containsIgnoringCase
import nl.brul.backend.shared.database.toSortOrder
import nl.brul.backend.shared.error.AppException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedCategoryRepository : CategoryRepository {
    override fun findById(id: UUID): Category? = transaction {
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }
            .singleOrNull()?.toCategory()
    }

    override fun findByName(name: String): Category? = transaction {
        CategoriesTable.selectAll().where { CategoriesTable.name eq name }
            .singleOrNull()?.toCategory()
    }

    override fun search(query: CategoryQuery): List<Category> = transaction {
        filtered(query)
            // The id as a second sort key keeps the order stable, so no category appears on two pages.
            .orderBy(
                sortColumn(query.sortField) to query.sortDirection.toSortOrder(),
                CategoriesTable.id to SortOrder.ASC
            )
            .limit(query.page.limit)
            .offset(query.page.offset.toLong())
            .map { it.toCategory() }
    }

    override fun count(query: CategoryQuery): Long = transaction {
        filtered(query).count()
    }

    override fun save(category: Category): Category {
        try {
            transaction {
                // Exposed retries a failed transaction; a duplicate name will not succeed on a retry.
                maxAttempts = 1
                CategoriesTable.insert {
                    it[id] = category.id
                    it[name] = category.name
                    it[createdAt] = category.createdAt
                }
            }
        } catch (exception: ExposedSQLException) {
            throw duplicateNameOr(exception)
        }
        return category
    }

    override fun update(category: Category): Boolean =
        try {
            transaction {
                maxAttempts = 1
                CategoriesTable.update({ CategoriesTable.id eq category.id }) {
                    it[name] = category.name
                } > 0
            }
        } catch (exception: ExposedSQLException) {
            throw duplicateNameOr(exception)
        }

    override fun delete(id: UUID): Boolean = transaction {
        CategoriesTable.deleteWhere { CategoriesTable.id eq id } > 0
    }

    /** Starts with every row and adds one `AND` condition for each filter that is set. */
    private fun filtered(query: CategoryQuery): Query {
        val rows = CategoriesTable.selectAll()
        query.text?.let { text -> rows.andWhere { CategoriesTable.name.containsIgnoringCase(text) } }
        return rows
    }

    private fun sortColumn(field: CategorySortField): Column<*> =
        when (field) {
            CategorySortField.NAME -> CategoriesTable.name
            CategorySortField.CREATED_AT -> CategoriesTable.createdAt
        }

    /**
     * The unique index is the final guard: if two requests with the same name pass the service check at the same
     * time, the database rejects the second one. That becomes 409; any other database error stays as it is.
     */
    private fun duplicateNameOr(exception: ExposedSQLException): Exception =
        if (exception.sqlState == UNIQUE_VIOLATION) {
            AppException.Conflict("Category name is already taken", "CATEGORY_NAME_TAKEN")
        } else {
            exception
        }

    private companion object {
        /** Standard SQL state for a unique constraint violation. */
        const val UNIQUE_VIOLATION = "23505"
    }
}
```

## Step 7: the service

All business rules: trimming and checking the name, refusing duplicates, generating id and timestamp.

```kotlin
// service/CategoryService.kt
package nl.brul.backend.features.category.service

import nl.brul.backend.features.category.dto.CategoryRequest
import nl.brul.backend.features.category.dto.CategoryResponse
import nl.brul.backend.features.category.mapper.toResponse
import nl.brul.backend.features.category.model.Category
import nl.brul.backend.features.category.model.CategoryQuery
import nl.brul.backend.features.category.repository.CategoryRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import nl.brul.backend.shared.paging.PageResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class CategoryService(private val repository: CategoryRepository) {
    fun search(query: CategoryQuery): PageResponse<CategoryResponse> =
        PageResponse(
            items = repository.search(query).map { it.toResponse() },
            total = repository.count(query),
            limit = query.page.limit,
            offset = query.page.offset
        )

    fun getById(id: UUID): CategoryResponse =
        findCategory(id).toResponse()

    fun create(request: CategoryRequest): CategoryResponse {
        val name = validName(request.name)
        requireNameIsFree(name)

        val category = Category(
            id = UUID.randomUUID(),
            name = name,
            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        return repository.save(category).toResponse()
    }

    fun rename(id: UUID, request: CategoryRequest): CategoryResponse {
        val existing = findCategory(id)
        val name = validName(request.name)
        // Keeping the same name is fine; only the name of another category is a conflict.
        if (name != existing.name) {
            requireNameIsFree(name)
        }

        val updated = existing.copy(name = name)
        repository.update(updated)
        return updated.toResponse()
    }

    fun delete(id: UUID) {
        if (!repository.delete(id)) {
            throw categoryNotFound()
        }
    }

    private fun validName(name: String): String {
        val trimmed = name.trim()
        requireValid(trimmed.isNotEmpty() && trimmed.length <= MAX_NAME_LENGTH) {
            "Name must contain between 1 and $MAX_NAME_LENGTH characters"
        }
        return trimmed
    }

    /** Gives a clear 409 in the normal case; the unique index in the database covers the rare race. */
    private fun requireNameIsFree(name: String) {
        if (repository.findByName(name) != null) {
            throw AppException.Conflict("Category name is already taken", "CATEGORY_NAME_TAKEN")
        }
    }

    private fun findCategory(id: UUID): Category =
        repository.findById(id) ?: throw categoryNotFound()

    private fun categoryNotFound() = AppException.NotFound("Category not found", "CATEGORY_NOT_FOUND")

    private companion object {
        /** Same as the column length in `CategoriesTable`. */
        const val MAX_NAME_LENGTH = 60
    }
}
```

## Step 8: the permission

In `backend/src/features/authorization/model/Permission.kt`, add one entry (mind the comma and the semicolon):

```kotlin
    FEEDBACK_DELETE("feedback.delete"),
    CATEGORY_MANAGE("category.manage");
```

`ADMIN` gets it automatically, because `DefaultRole.ADMIN` uses `Permission.entries`. At the next start the seeder
adds it to the database.

## Step 9: the routes

The access check is the first line of every route that needs one.

```kotlin
// route/CategoryRoutes.kt
package nl.brul.backend.features.category.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.category.dto.CategoryRequest
import nl.brul.backend.features.category.mapper.toCategoryQuery
import nl.brul.backend.features.category.service.CategoryService
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.http.uuidParameter

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)`. Every logged-in user may read categories;
 * creating, renaming and deleting needs `category.manage`.
 */
fun Route.categoryRoutes(categoryService: CategoryService, authorizationService: AuthorizationService) {
    route("/categories") {
        get {
            val query = call.request.queryParameters.toCategoryQuery()
            call.respond(categoryService.search(query))
        }

        get("/{id}") {
            call.respond(categoryService.getById(call.uuidParameter("id")))
        }

        post {
            authorizationService.requirePermission(call.currentUserId(), Permission.CATEGORY_MANAGE)
            val request = call.receive<CategoryRequest>()
            call.respond(HttpStatusCode.Created, categoryService.create(request))
        }

        patch("/{id}") {
            authorizationService.requirePermission(call.currentUserId(), Permission.CATEGORY_MANAGE)
            val id = call.uuidParameter("id")
            val request = call.receive<CategoryRequest>()
            call.respond(categoryService.rename(id, request))
        }

        delete("/{id}") {
            authorizationService.requirePermission(call.currentUserId(), Permission.CATEGORY_MANAGE)
            categoryService.delete(call.uuidParameter("id"))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

## Step 10: the wiring

Three files connect the new feature to the application.

**`backend/src/Application.kt`**: create the repository and the service, pass the service to the routing.

```kotlin
// Repositories
val categoryRepository = ExposedCategoryRepository()
...
// Services
val categoryService = CategoryService(categoryRepository)
...
configureRouting(
    authenticationService, userService, roleService, authorizationService, feedbackService, categoryService
)
```

**`backend/src/plugins/Routing.kt`**: accept the service and register the routes inside `authenticate`.

```kotlin
fun Application.configureRouting(
    authenticationService: AuthenticationService,
    userService: UserService,
    roleService: RoleService,
    authorizationService: AuthorizationService,
    feedbackService: FeedbackService,
    categoryService: CategoryService            // new
) {
    routing {
        ...
            authenticate(JWT_AUTHENTICATION) {
                ...
                feedbackRoutes(feedbackService, authorizationService)
                categoryRoutes(categoryService, authorizationService)   // new
            }
    }
}
```

**`backend/test/TestFixtures.kt`**: the tests wire everything too, with a fake.

```kotlin
class TestServices {
    ...
    val categoryRepository = FakeCategoryRepository()
    val categoryService = CategoryService(categoryRepository)
}

fun Application.configureTestApp(services: TestServices) {
    ...
    configureRouting(
        services.authenticationService,
        services.userService,
        services.roleService,
        services.authorizationService,
        services.feedbackService,
        services.categoryService                // new
    )
}
```

Add the imports IntelliJ suggests (Alt+Enter). Now the project compiles again.

## Step 11: tests

Create `backend/test/features/category/`.

### The fake

Like the database, the fake refuses a duplicate name. Otherwise the route tests could not test the 409.

```kotlin
// FakeCategoryRepository.kt
package nl.brul.backend.features.category

import nl.brul.backend.features.category.model.Category
import nl.brul.backend.features.category.model.CategoryQuery
import nl.brul.backend.features.category.model.CategorySortField
import nl.brul.backend.features.category.repository.CategoryRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/** In-memory [CategoryRepository]; like the database, it refuses a duplicate name. */
class FakeCategoryRepository : CategoryRepository {
    private val categories = mutableMapOf<UUID, Category>()

    override fun findById(id: UUID): Category? = categories[id]

    override fun findByName(name: String): Category? = categories.values.find { it.name == name }

    override fun search(query: CategoryQuery): List<Category> {
        val sorted = when (query.sortField) {
            CategorySortField.NAME -> matching(query).sortedBy { it.name }
            CategorySortField.CREATED_AT -> matching(query).sortedBy { it.createdAt }
        }
        val ordered = if (query.sortDirection == SortDirection.DESC) sorted.reversed() else sorted
        return ordered.drop(query.page.offset).take(query.page.limit)
    }

    override fun count(query: CategoryQuery): Long = matching(query).size.toLong()

    override fun save(category: Category): Category {
        requireUniqueName(category)
        categories[category.id] = category
        return category
    }

    override fun update(category: Category): Boolean {
        if (category.id !in categories) return false
        requireUniqueName(category)
        categories[category.id] = category
        return true
    }

    override fun delete(id: UUID): Boolean = categories.remove(id) != null

    private fun matching(query: CategoryQuery): List<Category> =
        categories.values.filter { query.text == null || it.name.contains(query.text, ignoreCase = true) }

    private fun requireUniqueName(category: Category) {
        if (categories.values.any { it.name == category.name && it.id != category.id }) {
            throw AppException.Conflict("Category name is already taken", "CATEGORY_NAME_TAKEN")
        }
    }
}
```

### Route tests

One test per status code you promised in the table at the top.

```kotlin
// CategoryRoutesTest.kt
package nl.brul.backend.features.category

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.TEST_ADMIN_EMAIL
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.errorCode
import nl.brul.backend.features.category.dto.CategoryResponse
import nl.brul.backend.register
import nl.brul.backend.shared.paging.PageResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryRoutesTest {

    private suspend fun ApplicationTestBuilder.createCategory(token: String, name: String): HttpResponse =
        client.post("/api/v1/categories") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name"}""")
        }

    @Test
    fun `request without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        val response = client.get("/api/v1/categories")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `admin creates a category and every user can read it`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")

        val created = createCategory(admin.accessToken, "Bugs")
        val list = client.get("/api/v1/categories") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Created, created.status)
        val page = Json.decodeFromString<PageResponse<CategoryResponse>>(list.bodyAsText())
        assertEquals(listOf("Bugs"), page.items.map { it.name })
    }

    @Test
    fun `regular user cannot create a category`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = createCategory(alice.accessToken, "Bugs")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `duplicate name returns 409`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        createCategory(admin.accessToken, "Bugs")

        val response = createCategory(admin.accessToken, "Bugs")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("CATEGORY_NAME_TAKEN", response.errorCode())
    }

    @Test
    fun `empty name returns 400 and unknown id returns 404`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)

        val empty = createCategory(admin.accessToken, "  ")
        val unknown = client.get("/api/v1/categories/${UUID.randomUUID()}") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.BadRequest, empty.status)
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("CATEGORY_NOT_FOUND", unknown.errorCode())
    }

    @Test
    fun `admin renames and deletes a category`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val id = Json.decodeFromString<CategoryResponse>(createCategory(admin.accessToken, "Bugs").bodyAsText()).id

        val rename = client.patch("/api/v1/categories/$id") {
            bearerAuth(admin.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Defects"}""")
        }
        val delete = client.delete("/api/v1/categories/$id") { bearerAuth(admin.accessToken) }
        val afterwards = client.get("/api/v1/categories/$id") { bearerAuth(admin.accessToken) }

        assertEquals("Defects", Json.decodeFromString<CategoryResponse>(rename.bodyAsText()).name)
        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(HttpStatusCode.NotFound, afterwards.status)
    }

    @Test
    fun `search matches part of the name and sorts A to Z by default`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        listOf("Feature request", "Bug", "Billing").forEach { createCategory(admin.accessToken, it) }

        val all = client.get("/api/v1/categories") { bearerAuth(admin.accessToken) }
        val search = client.get("/api/v1/categories?q=B") { bearerAuth(admin.accessToken) }

        val names = Json.decodeFromString<PageResponse<CategoryResponse>>(all.bodyAsText()).items.map { it.name }
        assertEquals(listOf("Billing", "Bug", "Feature request"), names)
        assertEquals(2, Json.decodeFromString<PageResponse<CategoryResponse>>(search.bodyAsText()).total)
    }
}
```

### A repository test

This one proves that the **real** database turns a duplicate into 409; the route tests only use the fake.

```kotlin
// CategoryRepositoryTest.kt
package nl.brul.backend.features.category

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.category.database.CategoriesTable
import nl.brul.backend.features.category.model.Category
import nl.brul.backend.features.category.repository.ExposedCategoryRepository
import nl.brul.backend.shared.error.AppException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryRepositoryTest {
    private val repository = ExposedCategoryRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:category_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(CategoriesTable) }
    }

    private fun newCategory(name: String = "Category ${UUID.randomUUID()}") =
        Category(UUID.randomUUID(), name, TEST_CREATED_AT)

    @Test
    fun `saves and finds a category by id and by name`() {
        val category = repository.save(newCategory())

        assertEquals(category, repository.findById(category.id))
        assertEquals(category, repository.findByName(category.name))
    }

    @Test
    fun `the unique index turns a duplicate name into 409`() {
        val category = repository.save(newCategory())

        val error = assertFailsWith<AppException.Conflict> { repository.save(newCategory(category.name)) }
        assertEquals("CATEGORY_NAME_TAKEN", error.code)
    }
}
```

Run `./kotlin test`. All tests, old and new, must pass.

## Step 12: try it against the real server

Restart the server and add a file `backend/http/6-categories.http`:

```http
### Create a category as admin
POST {{baseUrl}}/api/v1/categories
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "name": "Bugs"
}

> {%
    client.test("Admin can create a category", function () {
        client.assert(response.status === 201 || response.status === 409, "Got " + response.status);
    });
%}


### List categories as regular user
GET {{baseUrl}}/api/v1/categories?q=bug
Authorization: Bearer {{userToken}}

> {%
    client.test("User can list categories", function () {
        client.assert(response.status === 200, "Expected 200, got " + response.status);
    });
%}
```

(The first request accepts 409 too, so the file can be run more than once.)

## Step 13: finish the documentation

A feature is not done until others can use it:

- [ ] `openapi.yaml`: the five endpoints and the schemas `CategoryRequest`, `CategoryResponse`.
- [ ] `docs/implementation-plan.md`: the endpoints, the new permission, the new error codes.
- [ ] `features/category/README.md`: two or three lines on what the feature does.
- [ ] `TECHNICAL_DESIGN.md`: the permission matrix and the data model.

## Checklist for every new feature

- [ ] Only the folders you need; one class per file; names with the right suffix.
- [ ] Table in `DatabaseFactory`; `snake_case` column names.
- [ ] Permissions in `Permission`; granted in `DefaultRole` if not only for admins.
- [ ] Every protected route starts with an access check.
- [ ] Errors are `AppException` subclasses with stable codes; input checks use `requireValid()`.
- [ ] Every conversion in `mapper/`; DTOs never contain sensitive fields.
- [ ] Wired in `Application.kt` **and** `TestServices`.
- [ ] A fake that behaves like the real repository.
- [ ] Tests for 2xx, 400, 401, 403, 404 (and 409 if relevant).
- [ ] `.http` requests and `openapi.yaml` updated.

## Ideas to continue

- Link feedback to a category: add a nullable `category_id` column to `FeedbackTable` with
  `reference("category_id", CategoriesTable.id).nullable()`. This **changes an existing table**: delete
  `data/brul*`. What should happen to feedback when its category is deleted?
- Add a filter `?categoryId=` to `GET /feedback`.

## Check yourself

[Quiz, chapter 12](14-quiz.md#chapter-12-building-a-feature). Next: [chapter 13, design decisions](13-design-decisions.md).
