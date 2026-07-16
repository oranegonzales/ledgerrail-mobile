package dev.oranegonzales.ledgerrail.mobile.data

import com.squareup.moshi.JsonDataException
import dev.oranegonzales.ledgerrail.mobile.BuildConfig
import dev.oranegonzales.ledgerrail.mobile.data.api.ApiClient
import dev.oranegonzales.ledgerrail.mobile.data.api.ApiClientFactory
import dev.oranegonzales.ledgerrail.mobile.data.api.toDomain
import dev.oranegonzales.ledgerrail.mobile.data.api.toDto
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailFailure
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailRepository
import dev.oranegonzales.ledgerrail.mobile.domain.model.CreatedTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerSession
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import java.io.IOException
import java.io.Reader
import java.util.UUID
import okhttp3.ResponseBody
import retrofit2.Response

class NetworkLedgerRailRepository internal constructor(
    private val client: ApiClient,
) : LedgerRailRepository {

    constructor() : this(ApiClientFactory().create(BuildConfig.LEDGERRAIL_BASE_URL))

    override suspend fun checkConnection() {
        withFailures {
            val health = client.execute(client.api.health())
            if (health.status != "UP") {
                throw LedgerRailFailure(
                    "The LedgerRail service is not healthy",
                    isConnectionFailure = true,
                )
            }
        }
    }

    override suspend fun transfers(session: LedgerSession): List<Transfer> = withFailures {
        client.execute(client.api.transfers(session.accountId.toString(), TRANSFER_PAGE_SIZE))
            .map { it.toDomain() }
    }

    override suspend fun createTransfer(
        session: LedgerSession,
        idempotencyKey: String,
        request: NewTransfer,
    ): CreatedTransfer = withFailures {
        val response = client.api.createTransfer(idempotencyKey, request.toDto())
        val transfer = client.execute(response).toDomain()
        CreatedTransfer(
            transfer = transfer,
            replayed = response.headers()["Idempotency-Replayed"].toBoolean(),
        )
    }

    override suspend fun ledgerEntries(
        session: LedgerSession,
        transferId: UUID,
    ): List<LedgerEntry> = withFailures {
        client.execute(client.api.ledgerEntries(transferId.toString()))
            .map { it.toDomain() }
    }

    private fun <T> ApiClient.execute(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw LedgerRailFailure(
                "The server returned an empty response",
                isConnectionFailure = true,
            )
        }
        val problem = try {
            response.errorBody()?.readBounded()?.let(problemAdapter::fromJson)
        } catch (_: Exception) {
            null
        }
        val message = when (response.code()) {
            401 -> "This operation requires private operator access"
            429 -> safeDetail(problem?.detail) ?: "The public demo limit was reached. Try again later"
            409 -> safeDetail(problem?.detail) ?: "That idempotency key was used for a different request"
            502, 503, 504 -> "The free demo service is waking up. Wait a moment, then tap Retry"
            else -> safeDetail(problem?.detail)
                ?: safeDetail(problem?.title)
                ?: "LedgerRail returned HTTP ${response.code()}"
        }
        throw LedgerRailFailure(
            message = message,
            statusCode = response.code(),
            isConnectionFailure = response.code() in 300..399 || response.code() in 500..599,
        )
    }

    private suspend fun <T> withFailures(block: suspend () -> T): T = try {
        block()
    } catch (failure: LedgerRailFailure) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw LedgerRailFailure(
            failure.message ?: "The connection settings are invalid",
            cause = failure,
            isConnectionFailure = true,
        )
    } catch (failure: JsonDataException) {
        throw LedgerRailFailure(
            "The server returned an unexpected response",
            cause = failure,
            isConnectionFailure = true,
        )
    } catch (failure: IOException) {
        throw LedgerRailFailure(
            "The server could not be reached. A sleeping Render service can take about a minute to wake.",
            cause = failure,
            isConnectionFailure = true,
        )
    }

    private fun ResponseBody.readBounded(): String = charStream().use { reader ->
        reader.readAtMost(MAX_ERROR_BODY_CHARS)
    }

    private fun Reader.readAtMost(limit: Int): String {
        val output = StringBuilder(limit)
        val chunk = CharArray(1024)
        while (output.length < limit) {
            val count = read(chunk, 0, minOf(chunk.size, limit - output.length))
            if (count <= 0) break
            output.append(chunk, 0, count)
        }
        return output.toString()
    }

    private fun safeDetail(value: String?): String? = value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.take(240)

    private companion object {
        const val MAX_ERROR_BODY_CHARS = 4_096
        const val TRANSFER_PAGE_SIZE = 50
    }
}
