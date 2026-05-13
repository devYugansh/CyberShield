package com.nielit.cybershield.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val auth = FirebaseAuth.getInstance()
    private val guestModeKey = booleanPreferencesKey("is_guest_mode")

    val isGuestMode: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[guestModeKey] ?: false }

    suspend fun setGuestMode(isGuest: Boolean) {
        dataStore.edit { preferences ->
            preferences[guestModeKey] = isGuest
        }
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signOut() {
        auth.signOut()
    }
    
    suspend fun clearGuestMode() {
        setGuestMode(false)
    }
}
