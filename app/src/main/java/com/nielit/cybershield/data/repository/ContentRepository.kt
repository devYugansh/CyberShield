package com.nielit.cybershield.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nielit.cybershield.data.model.Module
import com.nielit.cybershield.data.model.ModuleProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for:
 *  - Module / lesson content  → loaded once from assets/modules.json
 *  - Lesson completion state  → persisted in DataStore as Set<String>
 */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ── Content ───────────────────────────────────────────────────────────

    /** Loaded once; safe to call repeatedly (result is the same object). */
    val modules: List<Module> by lazy {
        val raw = context.assets.open("modules.json").bufferedReader().readText()
        json.decodeFromString(raw)
    }

    fun moduleById(id: String): Module? = modules.find { it.id == id }

    // ── Progress ──────────────────────────────────────────────────────────

    /** Returns a Flow of all completed lesson IDs across all modules. */
    fun completedLessonsFlow(): Flow<Set<String>> =
        dataStore.data.map { prefs ->
            modules.flatMap { module ->
                val key = progressKey(module.id)
                prefs[key] ?: emptySet()
            }.toSet()
        }

    /**
     * Returns a Flow of ModuleProgress for every module.
     * Key per module: "progress_<moduleId>" → Set of completed lessonIds.
     */
    fun progressFlow(): Flow<Map<String, ModuleProgress>> =
        dataStore.data.map { prefs ->
            modules.associate { module ->
                val key = progressKey(module.id)
                val completedIds = prefs[key] ?: emptySet()
                module.id to ModuleProgress(
                    moduleId = module.id,
                    completed = completedIds.size,
                    total = module.lessons.size,
                )
            }
        }

    /** Call this when a user finishes the quiz at the end of a lesson. */
    suspend fun markLessonComplete(moduleId: String, lessonId: String) {
        dataStore.edit { prefs ->
            val key = progressKey(moduleId)
            val existing = prefs[key] ?: emptySet()
            prefs[key] = existing + lessonId
        }
    }

    private fun progressKey(moduleId: String) =
        stringSetPreferencesKey("progress_$moduleId")
}
