package dev.oranegonzales.ledgerrail.mobile.data

import com.squareup.moshi.JsonDataException
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
import java.util.UUID
import retrofit2.Response

class NetworkLedgerRailRepository internal constructor(
    private val clientFactory: ApiClientFactory,
) : LedgerRailRepository {

    constructor() : this(ApiClientFactory())

    override suspend fun checkConnection(serverUrl: String) {
        withFailures {
            val client = clientFactory.client(serverUrl)
            val health = client.execute(client.api.health())
            if (health.status != "UP") {
                throw LedgerRailFailure("The LedgerRail service is not healthy")
            }
        }
    }

    override suspend fun transfers(session: LedgerSession): List<Transfer> = withFailures {
        val client = clientFactory.client(session.serverUrl)
        client.execute(client.api.transfers(session.accountId.toString()))
            .map { it.toDomain() }
    }

    override suspend fun createTransfer(
        session: LedgerSession,
        idempotencyKey: String,
        request: NewTransfer,
    ): CreatedTransfer = withFailures {
        val client = clientFactory.client(session.serverUrl)
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
        val client = clientFactory.client(session.serverUrl)
        client.execute(client.api.ledgerEntries(transferId.toString()))
            .map { it.toDomain() }
    }

    private fun <T> ApiClient.execute(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw LedgerRailFailure("The server returned an empty response")
        }
        val problem = try {
            response.errorBody()?.string()?.let(problemAdapter::fromJson)
        } catch (_: Exception) {
            null
        }
        val message = when (response.code()) {
            401 -> "This operation requires private operator access"
            429 -> problem?.detail ?: "The public demo limit was reached. Try again later"
            409 -> problem?.detail ?: "That idempotency key was used for a different request"
            else -> problem?.detail ?: problem?.title ?: "LedgerRail returned HTTP ${response.code()}"
        }
        throw LedgerRailFailure(message, response.code())
    }

    private suspend fun <T> withFailures(block: suspend () -> T): T = try {
        block()
    } catch (failure: LedgerRailFailure) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw LedgerRailFailure(failure.message ?: "The connection settings are invalid", cause = failure)
    } catch (failure: JsonDataException) {
        throw LedgerRailFailure("The server returned an unexpected response", cause = failure)
    } catch (failure: IOException) {
        throw LedgerRailFailure(
            "The server could not be reached. A sleeping Render service can take about a minute to wake.",
            cause = failure,
        )
    }
}
