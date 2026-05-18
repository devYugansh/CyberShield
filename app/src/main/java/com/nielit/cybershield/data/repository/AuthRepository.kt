package com.nielit.cybershield.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.nielit.cybershield.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val auth = FirebaseAuth.getInstance()
    private val guestModeKey = booleanPreferencesKey("is_guest_mode")
    private val nameKey = stringPreferencesKey("user_name")
    private val emailKey = stringPreferencesKey("user_email")
    private val dobKey = stringPreferencesKey("user_dob")

    val isGuestMode: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[guestModeKey] ?: false }

    val currentUser: Flow<User?> = combine(
        isGuestMode, 
        authStateFlow(),
        dataStore.data
    ) { isGuest, firebaseUser, prefs ->
        when {
            isGuest -> User(isGuest = true, name = "Guest User")
            firebaseUser != null -> {
                val storedName = prefs[nameKey]
                val storedEmail = prefs[emailKey]
                val storedDob = prefs[dobKey]
                
                User(
                    phone = firebaseUser.phoneNumber,
                    name = storedName ?: firebaseUser.displayName,
                    email = storedEmail ?: firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    dob = storedDob,
                    isGuest = false
                )
            }
            else -> null
        }
    }

    private fun authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        if (isGuest) auth.signOut()
        dataStore.edit { preferences ->
            preferences[guestModeKey] = isGuest
            if (isGuest) {
                preferences.remove(nameKey)
                preferences.remove(emailKey)
                preferences.remove(dobKey)
            }
        }
    }

    suspend fun updateProfile(name: String, dob: String, email: String? = null) {
        dataStore.edit { preferences ->
            preferences[nameKey] = name
            preferences[dobKey] = dob
            if (email != null) preferences[emailKey] = email
        }
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signOut() {
        auth.signOut()
        // Note: we can't easily do suspend call in sync signOut, 
        // but typically the UI will trigger a clearGuestMode or similar.
    }
    
    suspend fun clearGuestMode() {
        setGuestMode(false)
    }
}
