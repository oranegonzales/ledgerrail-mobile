package dev.oranegonzales.ledgerrail.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import dev.oranegonzales.ledgerrail.mobile.ui.theme.LedgerRailTheme
import org.junit.Rule
import org.junit.Test

class LedgerRailAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launch_showsSandboxWarningAndAllowsPublicDemoCreation() {
        composeRule.setContent {
            LedgerRailTheme {
                LedgerRailApp(
                    state = LedgerRailUiState(isConnected = true, isLoading = false),
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

        composeRule.onNodeWithText("SYNTHETIC DATA ONLY").assertIsDisplayed()
        composeRule.onNodeWithText("Server URL").assertDoesNotExist()
        composeRule.onNodeWithTag("create-transfer")
            .performScrollTo()
            .assertIsEnabled()
    }
}
