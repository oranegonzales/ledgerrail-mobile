package dev.oranegonzales.ledgerrail.mobile.ui

import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import java.util.UUID

data class LedgerRailUiState(
    val serverUrl: String = "https://ledgerrail-core.onrender.com/",
    val apiKey: String = "",
    val accountId: String = UUID.randomUUID().toString(),
    val transferType: TransferType = TransferType.PAY_IN,
    val amount: String = "125.50",
    val currency: String = "JMD",
    val transfers: List<Transfer> = emptyList(),
    val selectedTransferId: UUID? = null,
    val ledgerEntries: List<LedgerEntry> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val replayAvailable: Boolean = false,
    val message: String = "Enter the private portfolio key to connect.",
    val isError: Boolean = false,
)
