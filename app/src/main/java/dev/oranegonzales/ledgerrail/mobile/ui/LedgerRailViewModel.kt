package dev.oranegonzales.ledgerrail.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailFailure
import dev.oranegonzales.ledgerrail.mobile.domain.LedgerRailRepository
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerSession
import dev.oranegonzales.ledgerrail.mobile.domain.model.NewTransfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LedgerRailViewModel(
    private val repository: LedgerRailRepository,
) : ViewModel() {

    private data class Submission(
        val idempotencyKey: String,
        val request: NewTransfer,
    )

    private val _uiState = MutableStateFlow(LedgerRailUiState())
    val uiState: StateFlow<LedgerRailUiState> = _uiState.asStateFlow()

    private var lastSubmission: Submission? = null
    private var activeJob: Job? = null
    private var operationGeneration = 0L

    init {
        connectAndRefresh()
    }

    fun onAccountIdChanged(value: String) {
        clearSubmission()
        _uiState.update {
            it.copy(
                accountId = value.take(36),
                transfers = emptyList(),
                selectedTransferId = null,
                ledgerEntries = emptyList(),
                message = "Account changed. Create a transfer or refresh its activity.",
                isError = false,
            )
        }
    }

    fun newAccount() {
        onAccountIdChanged(UUID.randomUUID().toString())
    }

    fun onTransferTypeChanged(value: TransferType) {
        _uiState.update { it.copy(transferType = value) }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amount = value.take(20)) }
    }

    fun onCurrencyChanged(value: String) {
        _uiState.update { it.copy(currency = value.uppercase().take(3)) }
    }

    fun connectAndRefresh() = launchOperation {
        val session = currentSession()
        _uiState.update { it.copy(isConnected = false, message = "Waking and checking LedgerRail…") }
        val transfers = connectWithWakeRetries(session)
        _uiState.update {
            it.copy(
                transfers = transfers,
                selectedTransferId = null,
                ledgerEntries = emptyList(),
                isConnected = true,
                message = "Connected. ${transfers.size} transfer${if (transfers.size == 1) "" else "s"} loaded.",
                isError = false,
            )
        }
    }

    private suspend fun connectWithWakeRetries(session: LedgerSession) = run {
        var attempt = 1
        while (true) {
            try {
                repository.checkConnection()
                return@run repository.transfers(session)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: LedgerRailFailure) {
                if (!exception.isConnectionFailure || attempt >= MAX_WAKE_ATTEMPTS) {
                    throw exception
                }
                val nextAttempt = attempt + 1
                _uiState.update {
                    it.copy(
                        message = "The free demo is still waking. Retrying automatically " +
                            "($nextAttempt/$MAX_WAKE_ATTEMPTS)…",
                        isError = false,
                    )
                }
                delay(WAKE_RETRY_DELAYS_MILLIS[attempt - 1])
                attempt = nextAttempt
            }
        }
    }

    fun createTransfer() = submit(replay = false)

    fun replayLastRequest() = submit(replay = true)

    fun selectTransfer(transferId: UUID) = launchOperation {
        val session = currentSession()
        val entries = repository.ledgerEntries(session, transferId)
        _uiState.update {
            it.copy(
                selectedTransferId = transferId,
                ledgerEntries = entries,
                message = "Loaded ${entries.size} ledger entries.",
                isError = false,
            )
        }
    }

    private fun submit(replay: Boolean) = launchOperation {
        val session = currentSession()
        if (!_uiState.value.isConnected) {
            throw LedgerRailFailure("Wait for LedgerRail to connect, or tap Retry")
        }
        val submission = if (replay) {
            lastSubmission ?: throw LedgerRailFailure("Create a transfer before replaying a request")
        } else {
            Submission(
                idempotencyKey = "mobile-${UUID.randomUUID()}",
                request = currentTransferRequest(session.accountId),
            ).also {
                lastSubmission = it
                _uiState.update { state -> state.copy(replayAvailable = true) }
            }
        }

        _uiState.update {
            it.copy(message = if (replay) "Replaying the exact request…" else "Creating transfer…")
        }
        val result = repository.createTransfer(
            session = session,
            idempotencyKey = submission.idempotencyKey,
            request = submission.request,
        )
        val transfers = repository.transfers(session)
        val entries = repository.ledgerEntries(session, result.transfer.id)
        _uiState.update {
            it.copy(
                transfers = transfers,
                selectedTransferId = result.transfer.id,
                ledgerEntries = entries,
                isConnected = true,
                message = if (result.replayed) {
                    "Safe replay returned the original transfer. No duplicate was created."
                } else {
                    "Transfer completed and both ledger entries were recorded."
                },
                isError = false,
            )
        }
    }

    private fun currentSession(): LedgerSession {
        val state = _uiState.value
        val accountId = try {
            UUID.fromString(state.accountId.trim())
        } catch (exception: IllegalArgumentException) {
            throw LedgerRailFailure("Enter a valid account UUID", cause = exception)
        }
        return LedgerSession(accountId = accountId)
    }

    private fun currentTransferRequest(accountId: UUID): NewTransfer {
        val state = _uiState.value
        val amount = state.amount.toBigDecimalOrNull()
            ?: throw LedgerRailFailure("Enter a valid amount")
        if (amount < BigDecimal("0.01")) {
            throw LedgerRailFailure("Amount must be at least 0.01")
        }
        if (amount.scale() > 2) {
            throw LedgerRailFailure("Amount can have at most two decimal places")
        }
        val integerDigits = (amount.precision() - amount.scale()).coerceAtLeast(0)
        if (integerDigits > 17) {
            throw LedgerRailFailure("Amount can have at most 17 digits before the decimal point")
        }
        val currency = state.currency.trim().uppercase()
        if (!currency.matches(Regex("[A-Z]{3}"))) {
            throw LedgerRailFailure("Currency must contain three letters")
        }
        return NewTransfer(
            accountId = accountId,
            type = state.transferType,
            amount = amount,
            currency = currency,
        )
    }

    private fun launchOperation(block: suspend () -> Unit) {
        val generation = ++operationGeneration
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        message = exception.message ?: "LedgerRail could not complete the request",
                        isError = true,
                        isConnected = if (
                            exception is LedgerRailFailure && exception.isConnectionFailure
                        ) {
                            false
                        } else {
                            it.isConnected
                        },
                    )
                }
            } finally {
                if (generation == operationGeneration) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun clearSubmission() {
        lastSubmission = null
        _uiState.update { it.copy(replayAvailable = false) }
    }

    companion object {
        private const val MAX_WAKE_ATTEMPTS = 3
        private val WAKE_RETRY_DELAYS_MILLIS = longArrayOf(2_000L, 5_000L)

        fun factory(repository: LedgerRailRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { LedgerRailViewModel(repository) }
        }
    }
}
