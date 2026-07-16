package dev.oranegonzales.ledgerrail.mobile.data

import dev.oranegonzales.ledgerrail.mobile.data.api.ApiClientFactory
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailFailure
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerSession
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkLedgerRailRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NetworkLedgerRailRepository

    private val accountId = UUID.fromString("6aa6aa37-52ea-4533-b319-339ecd090c4f")
    private val transferId = UUID.fromString("e38b13fd-64a2-49ae-b441-6c02cb409836")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = NetworkLedgerRailRepository(ApiClientFactory())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `health check accepts an up service`() = runTest {
        server.enqueue(jsonResponse("""{"status":"UP"}"""))

        repository.checkConnection(server.url("/").toString())

        assertEquals("/actuator/health/liveness", server.takeRequest().path)
    }

    @Test
    fun `create uses public demo and preserves idempotency and decimal values`() = runTest {
        server.enqueue(
            jsonResponse(transferJson()).setHeader("Idempotency-Replayed", "true"),
        )
        val session = session()

        val result = repository.createTransfer(
            session = session,
            idempotencyKey = "mobile-fixed-key",
            request = NewTransfer(
                accountId = accountId,
                type = TransferType.PAY_IN,
                amount = BigDecimal("125.50"),
                currency = "JMD",
            ),
        )

        val request = server.takeRequest()
        assertEquals("POST /api/v1/transfers HTTP/1.1", request.requestLine)
        assertNull(request.getHeader("X-Portfolio-Key"))
        assertEquals("mobile-fixed-key", request.getHeader("Idempotency-Key"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"accountId\":\"$accountId\""))
        assertTrue(body.contains("\"amount\":125.50"))
        assertEquals(transferId, result.transfer.id)
        assertEquals(BigDecimal("125.50"), result.transfer.amount)
        assertTrue(result.replayed)
    }

    @Test
    fun `problem detail is preserved for idempotency conflicts`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/problem+json")
                .setBody(
                    """{"title":"Conflict","status":409,"detail":"Key belongs to another payload"}""",
                ),
        )

        val failure = runCatching {
            repository.createTransfer(
                session = session(),
                idempotencyKey = "reused-key",
                request = NewTransfer(
                    accountId = accountId,
                    type = TransferType.PAY_OUT,
                    amount = BigDecimal("10.00"),
                    currency = "JMD",
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is LedgerRailFailure)
        val ledgerFailure = failure as LedgerRailFailure
        assertEquals(409, ledgerFailure.statusCode)
        assertEquals("Key belongs to another payload", ledgerFailure.message)
    }

    private fun session() = LedgerSession(
        serverUrl = server.url("/").toString(),
        accountId = accountId,
    )

    private fun transferJson() = """
        {
          "id": "$transferId",
          "accountId": "$accountId",
          "type": "PAY_IN",
          "amount": 125.50,
          "currency": "JMD",
          "status": "COMPLETED",
          "createdAt": "2026-07-14T22:21:43.666513249Z"
        }
    """.trimIndent()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
