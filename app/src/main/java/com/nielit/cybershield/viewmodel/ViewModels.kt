package com.nielit.cybershield.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
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
import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

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

fun String.maskPhone(): String {
    if (this.length < 4) return this
    return "****" + this.takeLast(4)
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = savedStateHandle.get<String>("verificationId")
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val _uiState    = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phone       = MutableStateFlow(savedStateHandle.get<String>("phone") ?: "")
    val phoneNumber: StateFlow<String> = _phone.asStateFlow()

    private val _otp         = MutableStateFlow("")
    val otpValue: StateFlow<String> = _otp.asStateFlow()

    private val _resendTimer = MutableStateFlow(30)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()

    private val _attempts    = MutableStateFlow(3)
    val attemptsLeft: StateFlow<Int> = _attempts.asStateFlow()

    private val _masked      = MutableStateFlow(savedStateHandle.get<String>("masked") ?: "")
    val maskedPhone: StateFlow<String> = _masked.asStateFlow()

    fun onPhoneChanged(value: String) {
        _phone.value = value
        savedStateHandle["phone"] = value
        _masked.value = value.maskPhone()
        savedStateHandle["masked"] = value.maskPhone()
    }
    fun onOtpChanged(value: String)   { _otp.value = value }

    fun setVerificationId(id: String) {
        storedVerificationId = id
        savedStateHandle["verificationId"] = id
    }

    fun requestOtp(activity: Activity) {
        if (_phone.value.length != 10) {
            _uiState.value = AuthUiState.Error("Invalid phone number")
            return
        }

        // --- Mock Bypass for Development ---
        if (_phone.value == "1234567890") {
            val masked = _phone.value.maskPhone()
            _masked.value = masked
            savedStateHandle["masked"] = masked
            savedStateHandle["verificationId"] = "test_verification_id"
            storedVerificationId = "test_verification_id"
            _uiState.value = AuthUiState.OtpSent("test_verification_id")
            startResendTimer()
            return
        }

        _uiState.value = AuthUiState.Loading
        val fullPhone = "+91${_phone.value}"

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                val masked = _phone.value.maskPhone()
                _masked.value = masked
                savedStateHandle["masked"] = masked
                savedStateHandle["verificationId"] = verificationId
                _uiState.value = AuthUiState.OtpSent(verificationId)
                startResendTimer()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val code = _otp.value

        // --- Mock Bypass for Development ---
        if (_phone.value == "1234567890" && code == "123456") {
            _uiState.value = AuthUiState.Verified
            return
        }

        if (code.length != 6 || storedVerificationId == null) {
            _uiState.value = AuthUiState.Error("Invalid OTP")
            return
        }

        _uiState.value = AuthUiState.Loading
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState.Verified
                } else {
                    _attempts.value -= 1
                    _uiState.value = AuthUiState.Error(task.exception?.localizedMessage ?: "Sign in failed")
                }
            }
    }

    fun resendOtp(activity: Activity) {
        if (resendToken == null) return
        
        _uiState.value = AuthUiState.Loading
        val fullPhone = "+91${_phone.value}"
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Resend failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                _resendTimer.value = 30
                _uiState.value = AuthUiState.OtpSent(verificationId)
                startResendTimer()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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
