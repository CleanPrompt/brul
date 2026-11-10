package nl.brul.backend.features.vote

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.features.feedback.dto.FeedbackResponse
import nl.brul.backend.features.vote.dto.VoteResponse
import nl.brul.backend.features.vote.dto.VoteSummaryResponse
import nl.brul.backend.features.vote.model.VoteType
import nl.brul.backend.register
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class VoteRoutesTest {

    private suspend fun ApplicationTestBuilder.createFeedbackId(token: String): String {
        val response = client.post("/api/v1/feedback") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"message":"A test feedback message"}""")
        }
        return Json.decodeFromString<FeedbackResponse>(response.bodyAsText()).id
    }

    private suspend fun ApplicationTestBuilder.putVote(
        feedbackId: String,
        token: String,
        type: String
    ): HttpResponse =
        client.put("/api/v1/feedback/$feedbackId/vote") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"type":"$type"}""")
        }

    private suspend fun HttpResponse.voteBody(): VoteResponse =
        Json.decodeFromString<VoteResponse>(bodyAsText())

    private suspend fun HttpResponse.summaryBody(): VoteSummaryResponse =
        Json.decodeFromString<VoteSummaryResponse>(bodyAsText())

    @Test
    fun `request without token returns 401 for authenticated endpoints`() = testApplication {
        application { configureTestApp(TestServices()) }

        val feedbackId = UUID.randomUUID().toString()

        val putResponse = client.put("/api/v1/feedback/$feedbackId/vote") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"LIKE"}""")
        }
        val getMeResponse = client.get("/api/v1/feedback/$feedbackId/vote/me")
        val deleteResponse = client.delete("/api/v1/feedback/$feedbackId/vote")

        assertEquals(HttpStatusCode.Unauthorized, putResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, getMeResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, deleteResponse.status)
    }

    @Test
    fun `public vote summary endpoint does not require authentication`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = client.get("/api/v1/feedback/$feedbackId/votes")

        assertEquals(HttpStatusCode.OK, response.status)
        val summary = response.summaryBody()
        assertEquals(0, summary.totalVotes)
        assertEquals(0, summary.likes)
        assertEquals(0, summary.dislikes)
        assertEquals(0, summary.score)
    }

    @Test
    fun `user casts LIKE vote returning 201 Created`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = putVote(feedbackId, bob.accessToken, "LIKE")

        assertEquals(HttpStatusCode.Created, response.status)
        val vote = response.voteBody()
        assertEquals(feedbackId, vote.feedbackId)
        assertEquals(bob.user.id, vote.userId)
        assertEquals(VoteType.LIKE, vote.type)
    }

    @Test
    fun `user changes vote to DISLIKE returning 200 OK`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val firstResponse = putVote(feedbackId, bob.accessToken, "LIKE")
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        val secondResponse = putVote(feedbackId, bob.accessToken, "DISLIKE")
        assertEquals(HttpStatusCode.OK, secondResponse.status)
        val vote = secondResponse.voteBody()
        assertEquals(firstResponse.voteBody().id, vote.id)
        assertEquals(VoteType.DISLIKE, vote.type)
    }

    @Test
    fun `owner cannot vote on own feedback returning 403 Forbidden`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = putVote(feedbackId, alice.accessToken, "LIKE")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `voting on unknown feedback returns 404`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val missingId = UUID.randomUUID().toString()

        val response = putVote(missingId, alice.accessToken, "LIKE")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `invalid uuid returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = putVote("not-a-uuid", alice.accessToken, "LIKE")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `invalid vote type returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = putVote(feedbackId, bob.accessToken, "INVALID")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `user can retrieve own vote`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        putVote(feedbackId, bob.accessToken, "LIKE")

        val response = client.get("/api/v1/feedback/$feedbackId/vote/me") {
            bearerAuth(bob.accessToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val vote = response.voteBody()
        assertEquals(bob.user.id, vote.userId)
        assertEquals(VoteType.LIKE, vote.type)
    }

    @Test
    fun `retrieving own vote returns 404 when user has not voted`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = client.get("/api/v1/feedback/$feedbackId/vote/me") {
            bearerAuth(bob.accessToken)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `user can delete own vote returning 204 NoContent`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        putVote(feedbackId, bob.accessToken, "LIKE")

        val deleteResponse = client.delete("/api/v1/feedback/$feedbackId/vote") {
            bearerAuth(bob.accessToken)
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = client.get("/api/v1/feedback/$feedbackId/vote/me") {
            bearerAuth(bob.accessToken)
        }
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `deleting vote when not voted returns 404`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val feedbackId = createFeedbackId(alice.accessToken)

        val response = client.delete("/api/v1/feedback/$feedbackId/vote") {
            bearerAuth(bob.accessToken)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `multiple users contribute to vote summary`() = testApplication {
        application { configureTestApp(TestServices()) }
        val author = register("author@example.com")
        val voter1 = register("voter1@example.com")
        val voter2 = register("voter2@example.com")
        val voter3 = register("voter3@example.com")
        val feedbackId = createFeedbackId(author.accessToken)

        putVote(feedbackId, voter1.accessToken, "LIKE")
        putVote(feedbackId, voter2.accessToken, "LIKE")
        putVote(feedbackId, voter3.accessToken, "DISLIKE")

        val response = client.get("/api/v1/feedback/$feedbackId/votes")

        assertEquals(HttpStatusCode.OK, response.status)
        val summary = response.summaryBody()
        assertEquals(2, summary.likes)
        assertEquals(1, summary.dislikes)
        assertEquals(3, summary.totalVotes)
        assertEquals(1, summary.score)
    }
}
