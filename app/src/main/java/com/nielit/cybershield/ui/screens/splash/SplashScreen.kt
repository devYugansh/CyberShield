package com.nielit.cybershield.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nielit.cybershield.ui.components.LoadingDots
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.SplashUiState
import com.nielit.cybershield.viewmodel.SplashViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-01: SplashScreen
// Stateful wrapper – owns ViewModel + navigation side-effects
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    onNavigateToHome : () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel        : SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToHome  -> onNavigateToHome()
            is SplashUiState.NavigateToLogin -> onNavigateToLogin()
            else -> Unit
        }
    }

    SplashContent()
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable – testable and preview-friendly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    val logoScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutBack),
        label         = "splashLogoScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(32.dp)
        ) {
            AnimatedShieldLogo(
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale)
            )

            Spacer(Modifier.height(20.dp))

            // App name
            Text(
                text  = "CyberShield",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Navy
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Tagline
            Text(
                text  = "By NIELIT",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            LoadingDots()
        }

        // NIELIT watermark pinned to bottom
        Text(
            text      = "Powered by NIELIT",
            style     = MaterialTheme.typography.labelSmall,
            color     = MutedText,
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnimatedShieldLogo  –  Shield SVG placeholder using Canvas
// Replace drawPath() calls with the actual NIELIT shield asset
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedShieldLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w,      h * 0.25f)
            lineTo(w,      h * 0.65f)
            quadraticBezierTo(w * 0.75f, h * 0.9f, w / 2f, h)
            quadraticBezierTo(w * 0.25f, h * 0.9f, 0f, h * 0.65f)
            lineTo(0f,     h * 0.25f)
            close()
        }
        drawPath(path = path, color = Navy)
        // Inner shield highlight
        val innerPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, h * 0.1f)
            lineTo(w * 0.85f, h * 0.32f)
            lineTo(w * 0.85f, h * 0.62f)
            quadraticBezierTo(w * 0.72f, h * 0.82f, w / 2f, h * 0.9f)
            quadraticBezierTo(w * 0.28f, h * 0.82f, w * 0.15f, h * 0.62f)
            lineTo(w * 0.15f, h * 0.32f)
            close()
        }
        drawPath(path = innerPath, color = Blue)
    }
}
