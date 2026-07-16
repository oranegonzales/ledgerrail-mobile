package dev.oranegonzales.ledgerrail.mobile.data.api

import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntryType
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferStatus
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import java.math.BigDecimal
import java.util.UUID

internal data class HealthDto(
    val status: String,
)

internal data class CreateTransferDto(
    val accountId: UUID,
    val type: TransferType,
    val amount: BigDecimal,
    val currency: String,
)

internal data class TransferDto(
    val id: UUID,
    val accountId: UUID,
    val type: TransferType,
    val amount: BigDecimal,
    val currency: String,
    val status: TransferStatus,
    val createdAt: String,
)

internal data class LedgerEntryDto(
    val id: UUID,
    val transferId: UUID,
    val accountCode: String,
    val entryType: LedgerEntryType,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: String,
)

internal data class ApiProblemDto(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
)

internal fun NewTransfer.toDto() = CreateTransferDto(
    accountId = accountId,
    type = type,
    amount = amount,
    currency = currency,
)

internal fun TransferDto.toDomain() = Transfer(
    id = id,
    accountId = accountId,
    type = type,
    amount = amount,
    currency = currency,
    status = status,
    createdAt = createdAt,
)

internal fun LedgerEntryDto.toDomain() = LedgerEntry(
    id = id,
    transferId = transferId,
    accountCode = accountCode,
    entryType = entryType,
    amount = amount,
    currency = currency,
    createdAt = createdAt,
)
