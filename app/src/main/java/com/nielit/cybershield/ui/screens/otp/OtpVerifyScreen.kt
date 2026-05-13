package com.nielit.cybershield.ui.screens.otp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nielit.cybershield.ui.components.CsErrorText
import com.nielit.cybershield.ui.components.CsPrimaryButton
import com.nielit.cybershield.ui.components.CsTopBar
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.AuthUiState
import com.nielit.cybershield.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-03: OtpVerifyScreen  –  stateful
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OtpVerifyScreen(
    verificationId: String,
    phone         : String,
    onVerified    : () -> Unit,
    onBack        : () -> Unit,
    viewModel     : AuthViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val otpValue     by viewModel.otpValue.collectAsState()
    val resendTimer  by viewModel.resendTimer.collectAsState()
    val attemptsLeft by viewModel.attemptsLeft.collectAsState()
    val maskedPhone  by viewModel.maskedPhone.collectAsState()
    val context      = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.phoneNumber.value.isEmpty()) {
            viewModel.onPhoneChanged(phone)
        }
        viewModel.setVerificationId(verificationId)
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Verified) onVerified()
    }

    OtpVerifyContent(
        maskedPhone   = maskedPhone,
        otpValue      = otpValue,
        onOtpChange   = viewModel::onOtpChanged,
        isLoading     = uiState is AuthUiState.Loading,
        errorMessage  = (uiState as? AuthUiState.Error)?.message.orEmpty(),
        resendSeconds = resendTimer,
        attemptsLeft  = attemptsLeft,
        onVerify      = { viewModel.verifyOtp() },
        onResend      = {
            val activity = context as? Activity
            if (activity != null) {
                viewModel.resendOtp(activity)
            }
        },
        onBack        = onBack
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OtpVerifyContent(
    maskedPhone  : String,
    otpValue     : String,
    onOtpChange  : (String) -> Unit,
    isLoading    : Boolean,
    errorMessage : String,
    resendSeconds: Int,
    attemptsLeft : Int,
    onVerify     : () -> Unit,
    onResend     : () -> Unit,
    onBack       : () -> Unit,
    modifier     : Modifier = Modifier
) {
    val hasError   = errorMessage.isNotBlank()
    val isLocked   = attemptsLeft <= 0

    Scaffold(
        topBar = {
            CsTopBar(
                title = "Verification",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = White
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text  = "Enter OTP",
                style = MaterialTheme.typography.headlineLarge,
                color = Navy
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Sent to +91 $maskedPhone",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )

            Spacer(Modifier.height(40.dp))

            // 6-box OTP input
            OtpInputRow(
                value      = otpValue,
                onChange   = onOtpChange,
                hasError   = hasError,
                isDisabled = isLocked
            )

            CsErrorText(
                message  = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )

            // Attempts hint – shown after first failure
            if (attemptsLeft in 1..2) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "$attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }

            Spacer(Modifier.height(28.dp))

            CsPrimaryButton(
                text      = "Verify",
                onClick   = onVerify,
                enabled   = otpValue.length == 6 && !isLocked,
                isLoading = isLoading
            )

            Spacer(Modifier.height(20.dp))

            // Resend row
            ResendTimerRow(
                seconds  = resendSeconds,
                onResend = onResend
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OtpInputRow  –  6 individual character boxes
// Internally uses a single hidden BasicTextField to capture input;
// the boxes are visual overlays driven by the string value.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OtpInputRow(
    value      : String,
    onChange   : (String) -> Unit,
    hasError   : Boolean = false,
    isDisabled : Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Shake animation on error
    val shakeOffset by animateFloatAsState(
        targetValue   = if (hasError) 0f else 0f,
        animationSpec = if (hasError) spring(dampingRatio = Spring.DampingRatioHighBouncy) else snap(),
        label         = "shakeAnimation"
    )

    Box {
        // Hidden capture field
        androidx.compose.foundation.text.BasicTextField(
            value         = value,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onChange(it) },
            singleLine    = true,
            enabled       = !isDisabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
        )

        // Visual boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer { translationX = shakeOffset }
        ) {
            repeat(6) { i ->
                val digit      = value.getOrNull(i)
                val isFocused  = value.length == i
                val borderColor= when {
                    hasError   -> ErrorRed
                    isFocused  -> Blue
                    digit != null -> Navy
                    else       -> Border
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = if (digit != null) Surface else White,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text  = digit?.toString() ?: "–",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = if (digit != null) Navy else MutedText.copy(alpha = 0.4f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ResendTimerRow  –  countdown + resend tap target
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ResendTimerRow(
    seconds : Int,
    onResend: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text  = "Didn't receive it? ",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        if (seconds > 0) {
            Text(
                text  = "Resend in ${seconds}s",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MutedText
            )
        } else {
            TextButton(
                onClick      = onResend,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text  = "Resend OTP",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Blue
                )
            }
        }
    }
}
