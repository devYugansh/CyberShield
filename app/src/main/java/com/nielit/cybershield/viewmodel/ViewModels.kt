package com.nielit.cybershield.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nielit.cybershield.domain.model.Lesson
import com.nielit.cybershield.domain.model.Module
import com.nielit.cybershield.domain.model.User
import com.nielit.cybershield.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.DrawerState
import com.google.firebase.auth.FirebaseAuth

// =============================================================================
// SplashViewModel  +  SplashUiState
// =============================================================================

sealed interface SplashUiState {
    object Loading          : SplashUiState
    object NavigateToHome   : SplashUiState
    object NavigateToLogin  : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    // inject: FirebaseAuth, or an AuthRepository abstraction
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            delay(1_500L)
            val hasSession = FirebaseAuth.getInstance().currentUser != null
            _uiState.value = if (hasSession) SplashUiState.NavigateToHome
            else            SplashUiState.NavigateToLogin
        }
    }
}

// =============================================================================
// AuthViewModel  +  AuthUiState  (shared by Login + OTP screens)
// =============================================================================

sealed interface AuthUiState {
    object Idle      : AuthUiState
    object Loading   : AuthUiState
    data class OtpSent(val verificationId: String) : AuthUiState
    object Verified  : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    // inject: AuthRepository
) : ViewModel() {

    private val _uiState    = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phone       = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phone.asStateFlow()

    private val _otp         = MutableStateFlow("")
    val otpValue: StateFlow<String> = _otp.asStateFlow()

    private val _resendTimer = MutableStateFlow(30)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()

    private val _attempts    = MutableStateFlow(3)
    val attemptsLeft: StateFlow<Int> = _attempts.asStateFlow()

    private val _masked      = MutableStateFlow("")
    val maskedPhone: StateFlow<String> = _masked.asStateFlow()

    fun onPhoneChanged(value: String) { _phone.value = value }
    fun onOtpChanged(value: String)   { _otp.value = value }

    fun requestOtp(/* verificationCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks */) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // TODO: FirebaseAuth.getInstance().verifyPhoneNumber(...)
            delay(1_000L)
            _masked.value = _phone.value.maskPhone()
            _uiState.value = AuthUiState.OtpSent(verificationId = "mock_verification_id")
            startResendTimer()
        }
    }

    fun verifyOtp(verificationId: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            delay(1_000L)
            // TODO: PhoneAuthProvider.getCredential(verificationId, otp) -> signInWithCredential
            if (_otp.value == "123456") {   // replace with real check
                _uiState.value = AuthUiState.Verified
            } else {
                _attempts.value -= 1
                _uiState.value = AuthUiState.Error("Wrong OTP. Try again.")
            }
        }
    }

    fun resendOtp(verificationId: String) {
        _resendTimer.value = 30
        startResendTimer()
        // TODO: call FirebaseAuth resend
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            for (i in 30 downTo 0) {
                _resendTimer.value = i
                if (i == 0) break
                delay(1_000L)
            }
        }
    }

    private fun String.maskPhone(): String {
        if (length < 5) return this
        return take(length - 5) + "XXXXX"
    }
}

// =============================================================================
// ThemeViewModel
// =============================================================================

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {
    val isDarkTheme: Flow<Boolean> = themeRepository.isDarkTheme

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkTheme(isDark)
        }
    }
}

// =============================================================================
// SettingsViewModel
// =============================================================================

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _notif      = MutableStateFlow(true)
    val notifEnabled: StateFlow<Boolean> = _notif.asStateFlow()

    private val _dark       = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _dark.asStateFlow()

    private val _masked     = MutableStateFlow("")
    val maskedPhone: StateFlow<String> = _masked.asStateFlow()

    private val _error      = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _error.asSharedFlow()

    init {
        viewModelScope.launch {
            // TODO: read DataStore and emit initial values
        }
    }

    fun setNotifEnabled(enabled: Boolean) {
        val prev = _notif.value
        _notif.value = enabled
        viewModelScope.launch {
            // TODO: DataStore write – revert to prev on failure
            //       _error.emit("Settings could not be saved.")
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkTheme(enabled)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // TODO: FirebaseAuth.signOut() + clear DataStore
        }
    }
}
