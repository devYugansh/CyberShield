package com.nielit.cybershield.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Palette ────────────────────────────────────────────────────────────
val Navy        = Color(0xFF1B3A6B)   // Primary – TopBar, headings, module headers
val Blue        = Color(0xFF2563EB)   // Accent  – CTAs, links, progress ring
val Surface     = Color(0xFFF5F7FA)   // Card backgrounds, alternating rows
val Border      = Color(0xFFCBD5E1)   // All card / table borders
val ErrorRed    = Color(0xFFB91C1C)   // Error text, error state backgrounds
val SuccessGreen= Color(0xFF166534)   // Correct quiz answer, completed checkmarks

// ── Semantic helpers ─────────────────────────────────────────────────────────
val MutedText   = Color(0xFF6B7280)   // Subtitles, hints, watermarks
val White       = Color(0xFFFFFFFF)
val Black       = Color(0xFF000000)
val Teal        = Color(0xFF0D9488)   // Avatar circle background
val ScrimBlack  = Color(0x4D000000)   // 30 % overlay for Side Drawer

// ── Progress Dash Bar ────────────────────────────────────────────────────────
val DashFilled  = Navy
val DashCurrent = Color(0xFF9CA3AF)   // mid-grey
val DashPending = Color(0xFFE5E7EB)   // light grey

// ── Quiz option states ───────────────────────────────────────────────────────
val OptionCorrect = Color(0xFFDCFCE7) // green-tinted bg
val OptionWrong   = Color(0xFFFEE2E2) // red-tinted bg
val OptionDefault = White
val OptionSelected= Color(0xFFEFF6FF) // light-blue selected
