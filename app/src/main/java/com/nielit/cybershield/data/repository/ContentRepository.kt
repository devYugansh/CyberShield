package com.nielit.cybershield.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nielit.cybershield.data.model.CourseData
import com.nielit.cybershield.data.model.CourseUnit
import com.nielit.cybershield.data.model.Module
import com.nielit.cybershield.data.model.ModuleProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for:
 *  - Course content (Units -> Modules -> Lessons) → loaded from assets/modules.json
 *  - Lesson completion state  → persisted in DataStore as Set<String>
 */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val UPDATED_JSON_NAME = "units_updated.json"
    private val ASSET_JSON_NAME = "units_v2.json"

    private val _courseData = MutableStateFlow(getCourseData())
    val courseDataFlow: StateFlow<CourseData> = _courseData.asStateFlow()

    private fun getCourseData(): CourseData {
        val updatedFile = File(context.filesDir, UPDATED_JSON_NAME)
        
        val rawJson = try {
            if (updatedFile.exists()) {
                updatedFile.readText()
            } else {
                context.assets.open(ASSET_JSON_NAME).bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            context.assets.open(ASSET_JSON_NAME).bufferedReader().use { it.readText() }
        }

        return try {
            json.decodeFromString<CourseData>(rawJson)
        } catch (e: Exception) {
            if (updatedFile.exists()) updatedFile.delete()
            val assetJson = context.assets.open(ASSET_JSON_NAME).bufferedReader().use { it.readText() }
            json.decodeFromString<CourseData>(assetJson)
        }
    }

    /** 
     * Force-reloads the content from disk. 
     * Call this after ContentUpdateManager downloads a new version.
     */
    fun refreshContent() {
        _courseData.value = getCourseData()
    }

    private val courseData: CourseData
        get() = _courseData.value

    /** All units in the course. */
    val units: List<CourseUnit> get() = courseData.units

    /** Current content version. */
    val contentVersion: Int get() = courseData.version

    /** Flattened list of all modules across all units. */
    val modules: List<Module> get() = units.flatMap { it.modules }

    fun moduleById(id: String): Module? = modules.find { it.id == id }

    // ── Progress ──────────────────────────────────────────────────────────

    /** Returns a Flow of all completed lesson IDs across all modules. */
    fun completedLessonsFlow(): Flow<Set<String>> =
        combine(courseDataFlow, dataStore.data) { data, prefs ->
            data.units.flatMap { it.modules }.flatMap { module ->
                val key = progressKey(module.id)
                prefs[key] ?: emptySet()
            }.toSet()
        }

    /**
     * Returns a Flow of ModuleProgress for every module.
     * Key per module: "progress_<moduleId>" → Set of completed lessonIds.
     */
    fun progressFlow(): Flow<Map<String, ModuleProgress>> =
        combine(courseDataFlow, dataStore.data) { data, prefs ->
            data.units.flatMap { it.modules }.associate { module ->
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
