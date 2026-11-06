# 11. Testing

Run all tests with `./kotlin test` (Windows: `kotlin.bat test`). This chapter explains the four kinds of tests in
the project and the tools behind them.

Files: `backend/test/`.

## The kinds of tests

| Kind | Tests | Needs | Example |
|---|---|---|---|
| Service unit test | Business rules | A fake repository | `FeedbackServiceTest` |
| Mapper / helper unit test | Conversions, parsing | Nothing | `UserMapperTest`, `QueryParametersTest` |
| Repository test | The real SQL | An in-memory H2 database | `FeedbackRepositoryTest` |
| Route (API) test | HTTP status codes, JSON, access rules | Ktor's test engine and fakes | `FeedbackRoutesTest` |

## Kotlin Test basics

```kotlin
class FeedbackServiceTest {
    private val repository = FakeFeedbackRepository()
    private val service = FeedbackService(repository)

    @Test
    fun `rejects an unknown status`() {
        val id = create()

        assertFailsWith<AppException.BadRequest> { service.updateStatus(id, UpdateFeedbackStatusRequest("DONE")) }
    }
}
```

- A test is a function with `@Test`.
- Names between backticks may contain spaces, so they read as sentences. Write them in English.
- `assertEquals(expected, actual)`, `assertTrue(...)`, `assertNull(...)` and `assertFailsWith<Type> { ... }`.
- Each test class gets a fresh instance for every test, so the fields (`repository`, `service`) start empty each time.

A good test has three parts, separated by a blank line: **arrange** (set up), **act** (do the one thing), **assert**
(check the result).

## Fakes instead of a database

A **fake** is a simple working implementation of an interface, here backed by a map:

```kotlin
// backend/test/features/feedback/FakeFeedbackRepository.kt (shortened)
class FakeFeedbackRepository : FeedbackRepository {
    private val items = mutableMapOf<UUID, Feedback>()

    override fun findById(id: UUID): Feedback? = items[id]
    override fun save(feedback: Feedback): Feedback = feedback.also { items[it.id] = it }
    override fun delete(id: UUID): Boolean = items.remove(id) != null
    ...
}
```

Because the service only knows the `FeedbackRepository` interface, it cannot tell the difference. Fakes make tests
fast (no database) and independent (every test starts empty).

**A fake must behave like the real thing.** If the real repository never changes the owner on `update`, the fake must
not either. If the real one refuses duplicate names, so must the fake. Otherwise your tests pass while the real
application fails.

## Route tests with `testApplication`

Ktor can run the whole application in memory, without a network:

```kotlin
// backend/test/features/feedback/FeedbackRoutesTest.kt
@Test
fun `owner and admin can read an item, another user cannot`() = testApplication {
    application { configureTestApp(TestServices()) }
    val admin = register(TEST_ADMIN_EMAIL)
    val alice = register("alice@example.com")
    val bob = register("bob@example.com")
    val id = createFeedbackId(alice.accessToken)

    val asOwner = client.get("/api/v1/feedback/$id") { bearerAuth(alice.accessToken) }
    val asAdmin = client.get("/api/v1/feedback/$id") { bearerAuth(admin.accessToken) }
    val asOther = client.get("/api/v1/feedback/$id") { bearerAuth(bob.accessToken) }

    assertEquals(HttpStatusCode.OK, asOwner.status)
    assertEquals(HttpStatusCode.OK, asAdmin.status)
    assertEquals(HttpStatusCode.Forbidden, asOther.status)
}
```

- `testApplication { }` starts a test server.
- `client.get(...)`, `client.post(...)` send real HTTP requests to it.
- `register(...)` is a helper that registers a user through the API and returns the token.

### `TestServices` and `configureTestApp`

```kotlin
// backend/test/TestFixtures.kt (shortened)
class TestServices {
    val userRepository = FakeUserRepository()
    val authorizationRepository = FakeAuthorizationRepository().also { AuthorizationSeeder(it).seed() }
    val jwtService = JwtService(testConfig())
    val authorizationService = AuthorizationService(authorizationRepository, userRepository)
    val feedbackRepository = FakeFeedbackRepository()
    val feedbackService = FeedbackService(feedbackRepository)
    ...
}

fun Application.configureTestApp(services: TestServices) {
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(services.jwtService, services.userRepository)
    configureRouting(services.authenticationService, ..., services.feedbackService)
}
```

`TestServices` is the same wiring as `Application.module()`, but with fakes. **Never call `module()` in a test**: it
creates the Exposed repositories, which need a real database.

`testConfig()` gives a fixed configuration with a test secret, so tests never read `.env`.

## Repository tests against H2

The SQL itself must be tested against a real database. H2 can run in memory:

```kotlin
class FeedbackRepositoryTest {
    private val repository = ExposedFeedbackRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:feedback_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, FeedbackTable) }
    }
    ...
}
```

`DB_CLOSE_DELAY=-1` keeps the in-memory database alive for the whole test class. That means **data from one test is
still there in the next**. The tests handle this by giving each test its own data: a new owner, or a unique random
marker in a name, and then filtering on it.

## What to test for every endpoint

The happy path and every relevant error:

| Status | Typical test |
|---|---|
| 2xx | The action works and returns the right JSON |
| 400 | Invalid input, invalid id, invalid query parameter |
| 401 | No token |
| 403 | Logged in without the permission; another user's item |
| 404 | Unknown id |
| 409 | Duplicate value (if the feature has a unique field) |

For lists, also test that **filters cannot widen access**.

## The `.http` files

Automated tests run against fakes. The `.http` files in `backend/http/` run against the **real** server and database.
Use them after a change to check the whole chain, including the SQL and the configuration.

## Check yourself

[Quiz, chapter 11](14-quiz.md#chapter-11-testing). Next: [chapter 12, the tutorial](12-tutorial-categories.md).
