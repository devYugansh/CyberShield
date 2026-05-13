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
// Removing hardcoded colors (Navy/MutedText) to allow theme-aware reactivity 
// and visibility on contrasting backgrounds (e.g. Navy TopBars).
val CyberShieldTypography = Typography(

    // Screen headings  e.g. "Welcome", "Enter OTP"
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 24.sp,
        lineHeight = 32.sp
    ),

    // Module card titles, section headers
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        lineHeight = 28.sp
    ),

    // App title in TopBar, card body titles
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        lineHeight = 26.sp
    ),

    // Lesson row titles, drawer items
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),

    // Body text – general content
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

    // Used for "Progress" and "01 / 03" labels
    labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),

    // Helper text, hints, watermarks
    labelSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        lineHeight = 16.sp
    ),

    // Quiz "QUESTION" muted caps label
    labelMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp
    )
)
