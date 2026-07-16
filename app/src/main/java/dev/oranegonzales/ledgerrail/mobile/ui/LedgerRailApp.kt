package dev.oranegonzales.ledgerrail.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.oranegonzales.ledgerrail.mobile.R
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntry
import dev.oranegonzales.ledgerrail.mobile.domain.model.LedgerEntryType
import dev.oranegonzales.ledgerrail.mobile.domain.model.Transfer
import dev.oranegonzales.ledgerrail.mobile.domain.model.TransferType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerRailApp(
    state: LedgerRailUiState,
    onServerUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onAccountIdChanged: (String) -> Unit,
    onNewAccount: () -> Unit,
    onTransferTypeChanged: (TransferType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onCreate: () -> Unit,
    onReplay: () -> Unit,
    onSelectTransfer: (UUID) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LedgerRail", fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { ConnectionPill(state.isConnected) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            MessageBanner(state)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth >= 840.dp) {
                    WideContent(
                        state = state,
                        onServerUrlChanged = onServerUrlChanged,
                        onApiKeyChanged = onApiKeyChanged,
                        onAccountIdChanged = onAccountIdChanged,
                        onNewAccount = onNewAccount,
                        onTransferTypeChanged = onTransferTypeChanged,
                        onAmountChanged = onAmountChanged,
                        onCurrencyChanged = onCurrencyChanged,
                        onConnect = onConnect,
                        onCreate = onCreate,
                        onReplay = onReplay,
                        onSelectTransfer = onSelectTransfer,
                    )
                } else {
                    CompactContent(
                        state = state,
                        onServerUrlChanged = onServerUrlChanged,
                        onApiKeyChanged = onApiKeyChanged,
                        onAccountIdChanged = onAccountIdChanged,
                        onNewAccount = onNewAccount,
                        onTransferTypeChanged = onTransferTypeChanged,
                        onAmountChanged = onAmountChanged,
                        onCurrencyChanged = onCurrencyChanged,
                        onConnect = onConnect,
                        onCreate = onCreate,
                        onReplay = onReplay,
                        onSelectTransfer = onSelectTransfer,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideContent(
    state: LedgerRailUiState,
    onServerUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onAccountIdChanged: (String) -> Unit,
    onNewAccount: () -> Unit,
    onTransferTypeChanged: (TransferType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onCreate: () -> Unit,
    onReplay: () -> Unit,
    onSelectTransfer: (UUID) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(0.9f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SandboxLabel() }
            item { ConnectionCard(state, onServerUrlChanged, onApiKeyChanged, onConnect) }
            item {
                TransferFormCard(
                    state,
                    onAccountIdChanged,
                    onNewAccount,
                    onTransferTypeChanged,
                    onAmountChanged,
                    onCurrencyChanged,
                    onCreate,
                    onReplay,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1.1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ActivityHeader(state, onConnect) }
            transferItems(state, onSelectTransfer)
            item { LedgerCard(state.ledgerEntries, state.selectedTransferId) }
        }
    }
}

@Composable
private fun CompactContent(
    state: LedgerRailUiState,
    onServerUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onAccountIdChanged: (String) -> Unit,
    onNewAccount: () -> Unit,
    onTransferTypeChanged: (TransferType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onCreate: () -> Unit,
    onReplay: () -> Unit,
    onSelectTransfer: (UUID) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SandboxLabel() }
        item { ConnectionCard(state, onServerUrlChanged, onApiKeyChanged, onConnect) }
        item {
            TransferFormCard(
                state,
                onAccountIdChanged,
                onNewAccount,
                onTransferTypeChanged,
                onAmountChanged,
                onCurrencyChanged,
                onCreate,
                onReplay,
            )
        }
        item { ActivityHeader(state, onConnect) }
        transferItems(state, onSelectTransfer)
        item { LedgerCard(state.ledgerEntries, state.selectedTransferId) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.transferItems(
    state: LedgerRailUiState,
    onSelectTransfer: (UUID) -> Unit,
) {
    if (state.transfers.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.no_transfers),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    } else {
        items(state.transfers, key = { it.id }) { transfer ->
            TransferCard(
                transfer = transfer,
                selected = transfer.id == state.selectedTransferId,
                onClick = { onSelectTransfer(transfer.id) },
            )
        }
    }
}

@Composable
private fun SandboxLabel() {
    Text(
        text = stringResource(R.string.sandbox_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ConnectionPill(connected: Boolean) {
    Surface(
        color = if (connected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Text(
            text = stringResource(if (connected) R.string.connected else R.string.not_connected),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MessageBanner(state: LedgerRailUiState) {
    Surface(
        color = if (state.isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = state.message,
            color = if (state.isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ConnectionCard(
    state: LedgerRailUiState,
    onServerUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onConnect: () -> Unit,
) {
    var revealKey by remember { mutableStateOf(false) }
    SectionCard {
        SectionTitle(stringResource(R.string.connection_title))
        Text(
            text = stringResource(R.string.connection_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text(stringResource(R.string.server_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChanged,
            label = { Text(stringResource(R.string.portfolio_key)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { revealKey = !revealKey }) {
                    Text(stringResource(if (revealKey) R.string.hide_key else R.string.show_key))
                }
            },
        )
        Button(
            onClick = onConnect,
            enabled = !state.isLoading && state.apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(stringResource(R.string.connect_refresh))
        }
    }
}

@Composable
private fun TransferFormCard(
    state: LedgerRailUiState,
    onAccountIdChanged: (String) -> Unit,
    onNewAccount: () -> Unit,
    onTransferTypeChanged: (TransferType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onCreate: () -> Unit,
    onReplay: () -> Unit,
) {
    SectionCard {
        SectionTitle(stringResource(R.string.transfer_title))
        OutlinedTextField(
            value = state.accountId,
            onValueChange = onAccountIdChanged,
            label = { Text(stringResource(R.string.account_id)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,
            trailingIcon = {
                TextButton(onClick = onNewAccount) {
                    Text(stringResource(R.string.new_account))
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.transferType == TransferType.PAY_IN,
                onClick = { onTransferTypeChanged(TransferType.PAY_IN) },
                label = { Text(stringResource(R.string.pay_in)) },
                enabled = !state.isLoading,
            )
            FilterChip(
                selected = state.transferType == TransferType.PAY_OUT,
                onClick = { onTransferTypeChanged(TransferType.PAY_OUT) },
                label = { Text(stringResource(R.string.pay_out)) },
                enabled = !state.isLoading,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.amount,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = state.currency,
                onValueChange = onCurrencyChanged,
                label = { Text(stringResource(R.string.currency)) },
                modifier = Modifier.widthIn(min = 100.dp, max = 130.dp),
                singleLine = true,
                enabled = !state.isLoading,
            )
        }
        Button(
            onClick = onCreate,
            enabled = !state.isLoading && state.apiKey.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create-transfer"),
        ) {
            Text(stringResource(R.string.create_transfer))
        }
        TextButton(
            onClick = onReplay,
            enabled = !state.isLoading && state.replayAvailable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.replay_request))
        }
        Text(
            text = stringResource(R.string.replay_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityHeader(state: LedgerRailUiState, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.activity_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${state.transfers.size} recorded",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(
            onClick = onRefresh,
            enabled = !state.isLoading && state.apiKey.isNotBlank(),
        ) {
            Text(stringResource(R.string.refresh))
        }
    }
}

@Composable
private fun TransferCard(transfer: Transfer, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (transfer.type == TransferType.PAY_IN) {
                        stringResource(R.string.pay_in)
                    } else {
                        stringResource(R.string.pay_out)
                    },
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatMoney(transfer.amount, transfer.currency),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = transfer.id.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatInstant(transfer.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LedgerCard(entries: List<LedgerEntry>, selectedTransferId: UUID?) {
    SectionCard {
        SectionTitle(stringResource(R.string.ledger_title))
        if (selectedTransferId == null) {
            Text(
                text = stringResource(R.string.select_transfer),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (entries.isEmpty()) {
            Text(stringResource(R.string.loading))
        } else {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.accountCode, fontWeight = FontWeight.SemiBold)
                        Text(
                            entry.entryType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatMoney(entry.amount, entry.currency),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = "Balanced: ${entries.hasBalancedPair()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

private fun formatMoney(amount: java.math.BigDecimal, currency: String): String {
    val displayAmount = if (amount.scale() <= 2) amount.setScale(2) else amount
    return "${displayAmount.toPlainString()} $currency"
}

private fun List<LedgerEntry>.hasBalancedPair(): Boolean =
    size == 2 &&
        map { it.entryType }.toSet() == setOf(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT) &&
        map { it.amount }.distinct().size == 1 &&
        map { it.currency }.distinct().size == 1

private fun formatInstant(value: String): String = try {
    DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
} catch (_: Exception) {
    value
}
