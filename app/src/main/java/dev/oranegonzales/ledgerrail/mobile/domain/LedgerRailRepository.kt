package dev.oranegonzales.ledgerrail.mobile.domain

import dev.oranegonzales.ledgerrail.mobile.domain.model.CreatedTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerSession
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import java.util.UUID

interface LedgerRailRepository {
    suspend fun checkConnection()

    suspend fun transfers(session: LedgerSession): List<Transfer>

    suspend fun createTransfer(
        session: LedgerSession,
        idempotencyKey: String,
        request: NewTransfer,
    ): CreatedTransfer

    suspend fun ledgerEntries(session: LedgerSession, transferId: UUID): List<LedgerEntry>
}

class LedgerRailFailure(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
    val isConnectionFailure: Boolean = false,
) : RuntimeException(message, cause)
