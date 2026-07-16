package dev.oranegonzales.ledgerrail.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.oranegonzales.ledgerrail.mobile.data.NetworkLedgerRailRepository
import dev.oranegonzales.ledgerrail.mobile.ui.LedgerRailApp
import dev.oranegonzales.ledgerrail.mobile.ui.LedgerRailViewModel
import dev.oranegonzales.ledgerrail.mobile.ui.theme.LedgerRailTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LedgerRailViewModel by viewModels {
        LedgerRailViewModel.factory(NetworkLedgerRailRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LedgerRailTheme {
                LedgerRailApp(
                    state = state,
                    onAccountIdChanged = viewModel::onAccountIdChanged,
                    onNewAccount = viewModel::newAccount,
                    onTransferTypeChanged = viewModel::onTransferTypeChanged,
                    onAmountChanged = viewModel::onAmountChanged,
                    onCurrencyChanged = viewModel::onCurrencyChanged,
                    onConnect = viewModel::connectAndRefresh,
                    onCreate = viewModel::createTransfer,
                    onReplay = viewModel::replayLastRequest,
                    onSelectTransfer = viewModel::selectTransfer,
                )
            }
        }
    }
}
