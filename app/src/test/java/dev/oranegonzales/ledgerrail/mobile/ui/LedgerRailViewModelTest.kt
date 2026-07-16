package dev.oranegonzales.ledgerrail.mobile.ui

import dev.oranegonzales.ledgerrail.mobile.MainDispatcherRule
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailRepository
import dev.oranegonzales.ledgerrail.mobile.domain.model.CreatedTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntryType
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerSession
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferStatus
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerRailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `connect loads account activity and marks session connected`() = runTest {
        val repository = FakeRepository()
        val viewModel = LedgerRailViewModel(repository)

        advanceUntilIdle()

        assertEquals(1, repository.healthChecks)
        assertTrue(viewModel.uiState.value.isConnected)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(viewModel.uiState.value.accountId, repository.lastSession?.accountId.toString())
        assertTrue(viewModel.uiState.value.message.startsWith("Connected."))
    }

    @Test
    fun `replay preserves both idempotency key and original payload`() = runTest {
        val repository = FakeRepository()
        val viewModel = LedgerRailViewModel(repository)
        advanceUntilIdle()

        viewModel.createTransfer()
        advanceUntilIdle()
        val firstKey = repository.createCalls.single().first
        val firstRequest = repository.createCalls.single().second

        viewModel.onAmountChanged("999.00")
        viewModel.replayLastRequest()
        advanceUntilIdle()

        assertEquals(2, repository.createCalls.size)
        assertEquals(firstKey, repository.createCalls.last().first)
        assertEquals(firstRequest, repository.createCalls.last().second)
        assertTrue(viewModel.uiState.value.message.contains("No duplicate"))
        assertEquals(1, viewModel.uiState.value.transfers.size)
        assertEquals(2, viewModel.uiState.value.ledgerEntries.size)
    }

    @Test
    fun `changing account disables replay without disconnecting the service`() = runTest {
        val repository = FakeRepository()
        val viewModel = LedgerRailViewModel(repository)
        advanceUntilIdle()
        viewModel.createTransfer()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.replayAvailable)

        viewModel.onAccountIdChanged(UUID.randomUUID().toString())

        assertFalse(viewModel.uiState.value.replayAvailable)
        assertTrue(viewModel.uiState.value.isConnected)
    }

    @Test
    fun `failed automatic connection exposes a working retry`() = runTest {
        val repository = FakeRepository(healthFailures = 1)
        val viewModel = LedgerRailViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.isError)

        viewModel.connectAndRefresh()
        advanceUntilIdle()

        assertEquals(2, repository.healthChecks)
        assertTrue(viewModel.uiState.value.isConnected)
        assertFalse(viewModel.uiState.value.isError)
    }

    @Test
    fun `connection failure during submission marks the service offline`() = runTest {
        val repository = FakeRepository(createConnectionFailures = 1)
        val viewModel = LedgerRailViewModel(repository)
        advanceUntilIdle()

        viewModel.createTransfer()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.isError)
    }

    @Test
    fun `local validation failure does not mark the service offline`() = runTest {
        val viewModel = LedgerRailViewModel(FakeRepository())
        advanceUntilIdle()

        viewModel.onAmountChanged("not-a-number")
        viewModel.createTransfer()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.isError)
    }

    private class FakeRepository(
        private var healthFailures: Int = 0,
        private var createConnectionFailures: Int = 0,
    ) : LedgerRailRepository {
        val createCalls = mutableListOf<Pair<String, NewTransfer>>()
        var lastSession: LedgerSession? = null
        var healthChecks: Int = 0
        private val transfers = mutableListOf<Transfer>()
        private val transferId = UUID.fromString("e38b13fd-64a2-49ae-b441-6c02cb409836")

        override suspend fun checkConnection() {
            healthChecks++
            if (healthFailures > 0) {
                healthFailures--
                throw dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailFailure(
                    "Service unavailable",
                    isConnectionFailure = true,
                )
            }
        }

        override suspend fun transfers(session: LedgerSession): List<Transfer> {
            lastSession = session
            return transfers.toList()
        }

        override suspend fun createTransfer(
            session: LedgerSession,
            idempotencyKey: String,
            request: NewTransfer,
        ): CreatedTransfer {
            if (createConnectionFailures > 0) {
                createConnectionFailures--
                throw dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailFailure(
                    "Connection lost",
                    isConnectionFailure = true,
                )
            }
            lastSession = session
            val replayed = createCalls.any { it.first == idempotencyKey }
            createCalls += idempotencyKey to request
            val transfer = Transfer(
                id = transferId,
                accountId = request.accountId,
                type = request.type,
                amount = request.amount,
                currency = request.currency,
                status = TransferStatus.COMPLETED,
                createdAt = "2026-07-14T22:21:43Z",
            )
            if (!replayed) transfers += transfer
            return CreatedTransfer(transfer = transfer, replayed = replayed)
        }

        override suspend fun ledgerEntries(
            session: LedgerSession,
            transferId: UUID,
        ): List<LedgerEntry> = listOf(
            LedgerEntry(
                id = UUID.fromString("911e8dc0-dc12-4db9-a2e3-b884e37712eb"),
                transferId = transferId,
                accountCode = "CASH",
                entryType = LedgerEntryType.DEBIT,
                amount = BigDecimal("125.50"),
                currency = "JMD",
                createdAt = "2026-07-14T22:21:43Z",
            ),
            LedgerEntry(
                id = UUID.fromString("cc1a329d-14a9-4e7f-9be9-f1da5804c129"),
                transferId = transferId,
                accountCode = "CUSTOMER_FUNDS",
                entryType = LedgerEntryType.CREDIT,
                amount = BigDecimal("125.50"),
                currency = "JMD",
                createdAt = "2026-07-14T22:21:43Z",
            ),
        )
    }
}
