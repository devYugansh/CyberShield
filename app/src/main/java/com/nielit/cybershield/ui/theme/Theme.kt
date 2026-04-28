package com.nielit.cybershield.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color as ComposeColor


// ── Shape tokens (4dp grid; 12dp cards, 8dp small, 50% pills) ─────────────────
val CyberShieldShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // chips / snackbar
    small      = RoundedCornerShape(8.dp),    // small elements
    medium     = RoundedCornerShape(12.dp),   // cards (primary token)
    large      = RoundedCornerShape(16.dp),   // bottom sheets
    extraLarge = RoundedCornerShape(50)        // pills / FABs
)

// ── Light colour scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Blue,
    onPrimary        = White,
    primaryContainer = Surface,
    secondary        = Navy,
    onSecondary      = White,
    background       = Surface,
    onBackground     = Navy,
    surface          = White,
    onSurface        = Navy,
    error            = ErrorRed,
    onError          = White,
    outline          = Border
)

// ── Dark colour scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Blue,
    onPrimary        = White,
    primaryContainer = ComposeColor(0xFF1E293B),
    secondary        = ComposeColor(0xFF93C5FD),
    onSecondary      = ComposeColor(0xFF1E293B),
    background       = ComposeColor(0xFF0F172A),
    onBackground     = White,
    surface          = ComposeColor(0xFF1E293B),
    onSurface        = White,
    error            = ComposeColor(0xFFFCA5A5),
    onError          = ComposeColor(0xFF7F1D1D),
    outline          = ComposeColor(0xFF334155)
)

// ── Theme entry-point ──────────────────────────────────────────────────────────
@Composable
fun CyberShieldTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = CyberShieldTypography,
        shapes      = CyberShieldShapes,
        content     = content
    )
}

// Suppress import warning on Color reference in DarkColorScheme above
