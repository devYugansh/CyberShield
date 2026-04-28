package com.nielit.cybershield.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Font families ─────────────────────────────────────────────────────────────
// Add Poppins & Nunito .ttf files to res/font/ and register them here.
// Falling back to system defaults for compilation safety.
val PoppinsFontFamily = FontFamily.Default   // replace: FontFamily(Font(R.font.poppins_regular), ...)
val NunitoFontFamily  = FontFamily.Default   // replace: FontFamily(Font(R.font.nunito_regular), ...)

// ── CyberShield Typography ────────────────────────────────────────────────────
val CyberShieldTypography = Typography(

    // Screen headings  e.g. "Welcome", "Enter OTP"
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 24.sp,
        lineHeight = 32.sp,
        color      = Navy
    ),

    // Module card titles, section headers
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        lineHeight = 28.sp,
        color      = Navy
    ),

    // App title in TopBar, card body titles (14sp bold)
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        color      = Navy
    ),

    // Lesson row titles, drawer items
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),

    // Body text – flashcard body (12sp, line-height 1.5)
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 21.sp
    ),

    bodySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 18.sp
    ),

    // Helper text, hints, watermarks (10sp)
    labelSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 10.sp,
        lineHeight = 14.sp,
        color      = MutedText
    ),

    // Quiz "QUESTION" muted caps label (10sp)
    labelMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
)
