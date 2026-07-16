package dev.oranegonzales.ledgerrail.mobile.domain.model

import java.math.BigDecimal
import java.util.UUID

data class LedgerSession(
    val serverUrl: String,
    val accountId: UUID,
)

enum class TransferType {
    PAY_IN,
    PAY_OUT,
}

enum class TransferStatus {
    COMPLETED,
}

data class Transfer(
    val id: UUID,
    val accountId: UUID,
    val type: TransferType,
    val amount: BigDecimal,
    val currency: String,
    val status: TransferStatus,
    val createdAt: String,
)

enum class LedgerEntryType {
    DEBIT,
    CREDIT,
}

data class LedgerEntry(
    val id: UUID,
    val transferId: UUID,
    val accountCode: String,
    val entryType: LedgerEntryType,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: String,
)

data class NewTransfer(
    val accountId: UUID,
    val type: TransferType,
    val amount: BigDecimal,
    val currency: String,
)

data class CreatedTransfer(
    val transfer: Transfer,
    val replayed: Boolean,
)
