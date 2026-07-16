package dev.oranegonzales.ledgerrail.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = RailTeal,
    onPrimary = RailNavy,
    primaryContainer = RailTealDark,
    onPrimaryContainer = RailCream,
    secondary = RailSlate,
    background = RailNavy,
    onBackground = RailCream,
    surface = RailNavySoft,
    onSurface = RailCream,
    surfaceVariant = RailNavySoft,
    onSurfaceVariant = RailSlate,
    error = RailError,
    onError = RailErrorDark,
)

private val LightColors = lightColorScheme(
    primary = RailTealDark,
    onPrimary = RailCream,
    primaryContainer = RailTeal,
    onPrimaryContainer = RailNavy,
    secondary = RailNavySoft,
    background = RailCream,
    onBackground = RailInk,
    surface = ColorTokens.surfaceLight,
    onSurface = RailInk,
    surfaceVariant = ColorTokens.surfaceVariantLight,
    onSurfaceVariant = RailNavySoft,
    error = RailErrorDark,
)

private object ColorTokens {
    val surfaceLight = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val surfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFDCE8E5)
}

@Composable
fun LedgerRailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
