package com.nielit.cybershield.ui.screens.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import android.app.Activity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.nielit.cybershield.R
import com.nielit.cybershield.ui.components.CsErrorText
import com.nielit.cybershield.ui.components.CsPrimaryButton
import com.nielit.cybershield.ui.screens.splash.AnimatedShieldLogo
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.AuthUiState
import com.nielit.cybershield.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-02: LoginScreen  –  stateful wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onOtpSent: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val phoneState by viewModel.phoneNumber.collectAsState()
    val context    = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                viewModel.signInWithGoogle(
                    idToken = account.idToken ?: "",
                    name = account.displayName,
                    email = account.email
                )
            } catch (e: ApiException) {
                // Handle error
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.OtpSent -> onOtpSent((uiState as AuthUiState.OtpSent).verificationId)
            is AuthUiState.Verified -> onLoginSuccess()
            else -> Unit
        }
    }

    LoginContent(
        phone       = phoneState,
        onPhoneChange = viewModel::onPhoneChanged,
        isLoading   = uiState is AuthUiState.Loading,
        errorMessage= (uiState as? AuthUiState.Error)?.message.orEmpty(),
        onGetOtp    = {
            val activity = context as? Activity
            if (activity != null) {
                viewModel.requestOtp(activity)
            }
        },
        onGoogleLogin = {
            val activity = context as? Activity
            if (activity != null) {
                googleSignInLauncher.launch(viewModel.getGoogleSignInIntent(activity))
            }
        },
        onGuestLogin = { viewModel.loginAsGuest() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginContent(
    phone        : String,
    onPhoneChange: (String) -> Unit,
    isLoading    : Boolean,
    errorMessage : String,
    onGetOtp     : () -> Unit,
    onGoogleLogin: () -> Unit,
    onGuestLogin : () -> Unit,
    modifier     : Modifier = Modifier
) {
    val focusManager   = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(56.dp))

        // App Logo (smaller, above form)
        AnimatedShieldLogo(modifier = Modifier.size(56.dp))

        Spacer(Modifier.height(32.dp))

        // Heading block
        Text(
            text  = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Enter your mobile number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Phone input row
        PhoneInputRow(
            phone         = phone,
            onPhoneChange = { value ->
                // Accept only digits, max 10
                if (value.length <= 10 && value.all { it.isDigit() }) onPhoneChange(value)
            },
            hasError      = errorMessage.isNotBlank(),
            focusRequester= focusRequester,
            onDone        = {
                focusManager.clearFocus()
                if (phone.length == 10) onGetOtp()
            }
        )

        CsErrorText(
            message  = errorMessage,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // CTA button
        CsPrimaryButton(
            text      = "Get OTP",
            onClick   = { focusManager.clearFocus(); onGetOtp() },
            enabled   = phone.length == 10,
            isLoading = isLoading
        )

        Spacer(Modifier.height(16.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                " OR ",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Google Login Button
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Assuming you have a google logo in your res/drawable
                // If not, replace with text or a generic icon
                // Icon(Icons.Default.Google, contentDescription = null)
                Text("Continue with Google", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGuestLogin,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Continue as Guest", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        // Hint text
        Text(
            text      = "We will send a 6-digit code via SMS",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PhoneInputRow  –  +91 pill + 10-digit text field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhoneInputRow(
    phone         : String,
    onPhoneChange : (String) -> Unit,
    hasError      : Boolean        = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onDone        : () -> Unit     = {}
) {
    val borderColor = if (hasError) MaterialTheme.colorScheme.error 
                     else if (phone.length == 10) MaterialTheme.colorScheme.primary 
                     else MaterialTheme.colorScheme.outline

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Country code pill (+91) – read-only
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text  = "+91",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        VerticalDivider(
            modifier  = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.outline
        )


        // Phone number input
        BasicTextField(
            value       = phone,
            onValueChange = onPhoneChange,
            textStyle   = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine  = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction    = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox  = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    if (phone.isEmpty()) {
                        Text("98765 XXXXX", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium)
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(focusRequester)
        )
    }
}

// Alias for BasicTextField without additional import conflicts
@Composable
private fun BasicTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    textStyle     : androidx.compose.ui.text.TextStyle,
    singleLine    : Boolean,
    keyboardOptions : KeyboardOptions,
    keyboardActions : KeyboardActions,
    decorationBox : @Composable (@Composable () -> Unit) -> Unit,
    modifier      : Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value           = value,
        onValueChange   = onValueChange,
        textStyle       = textStyle,
        singleLine      = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox   = decorationBox,
        modifier        = modifier
    )
}
