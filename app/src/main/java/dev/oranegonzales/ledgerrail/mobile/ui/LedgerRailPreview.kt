package dev.oranegonzales.ledgerrail.mobile.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntryType
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferStatus
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import dev.oranegonzales.ledgerrail.mobile.ui.theme.LedgerRailTheme
import java.math.BigDecimal
import java.util.UUID

private val PreviewAccountId = UUID.fromString("6aa6aa37-52ea-4533-b319-339ecd090c4f")
private val PreviewTransferId = UUID.fromString("e38b13fd-64a2-49ae-b441-6c02cb409836")

@Preview(name = "Phone · light", showBackground = true, widthDp = 412, heightDp = 915)
@Preview(
    name = "Phone · dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LedgerRailPhonePreview() {
    LedgerRailTheme {
        LedgerRailApp(
            state = previewState(),
            onServerUrlChanged = {},
            onAccountIdChanged = {},
            onNewAccount = {},
            onTransferTypeChanged = {},
            onAmountChanged = {},
            onCurrencyChanged = {},
            onConnect = {},
            onCreate = {},
            onReplay = {},
            onSelectTransfer = {},
        )
    }
}

private fun previewState(): LedgerRailUiState {
    val transfer = Transfer(
        id = PreviewTransferId,
        accountId = PreviewAccountId,
        type = TransferType.PAY_IN,
        amount = BigDecimal("125.50"),
        currency = "JMD",
        status = TransferStatus.COMPLETED,
        createdAt = "2026-07-14T22:21:43Z",
    )
    return LedgerRailUiState(
        accountId = PreviewAccountId.toString(),
        transfers = listOf(transfer),
        selectedTransferId = PreviewTransferId,
        ledgerEntries = listOf(
            previewEntry("911e8dc0-dc12-4db9-a2e3-b884e37712eb", "CASH", LedgerEntryType.DEBIT),
            previewEntry(
                "cc1a329d-14a9-4e7f-9be9-f1da5804c129",
                "CUSTOMER_FUNDS",
                LedgerEntryType.CREDIT,
            ),
        ),
        isConnected = true,
        replayAvailable = true,
        message = "Transfer completed and both ledger entries were recorded.",
    )
}

private fun previewEntry(id: String, accountCode: String, type: LedgerEntryType) = LedgerEntry(
    id = UUID.fromString(id),
    transferId = PreviewTransferId,
    accountCode = accountCode,
    entryType = type,
    amount = BigDecimal("125.50"),
    currency = "JMD",
    createdAt = "2026-07-14T22:21:43Z",
)
