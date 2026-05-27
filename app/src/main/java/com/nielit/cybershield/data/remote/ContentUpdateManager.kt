package com.nielit.cybershield.data.remote

import android.content.Context
import android.util.Log
import com.nielit.cybershield.data.model.CourseData
import com.nielit.cybershield.data.repository.ContentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val repository: ContentRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "ContentUpdateManager"

    private val _updateEvents = MutableSharedFlow<UpdateEvent>()
    val updateEvents = _updateEvents.asSharedFlow()
    
    // URL for the Public Content Repository (pointing to 'ota' folder in this repo)
    private val BASE_URL = "https://raw.githubusercontent.com/devYugansh/CyberShield/master/ota"
    private val VERSION_URL = "$BASE_URL/version.json"
    private val CONTENT_URL = "$BASE_URL/units.json"
    private val IMAGES_BASE_URL = "$BASE_URL/images"

    /**
     * Checks if a new version is available and downloads it.
     * returns true if a new version was successfully downloaded.
     */
    suspend fun checkForUpdates(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(VERSION_URL)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    
                    val versionInfo = response.body?.string() ?: return@withContext false
                    val remoteVersion = json.decodeFromString<VersionMetadata>(versionInfo).latest_content_version
                    val currentVersion = repository.contentVersion

                    Log.d(TAG, "Remote version: $remoteVersion, Current version: $currentVersion")

                    if (remoteVersion > currentVersion) {
                        return@withContext downloadAndApplyUpdate()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
            false
        }
    }

    private suspend fun downloadAndApplyUpdate(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updateFile = File(context.filesDir, "units_updated.json")
                val request = Request.Builder()
                    .url(CONTENT_URL)
                    .build()
                
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    
                    val rawJson = response.body?.string() ?: return@withContext false

                    // 2. VALIDATION: Parse it to ensure it's valid before saving
                    val validatedData = json.decodeFromString<CourseData>(rawJson)
                    
                    if (validatedData.version > repository.contentVersion) {
                        // 3. Download missing images before applying JSON
                        val imageNames = extractImageNames(validatedData)
                        downloadMissingImages(imageNames)

                        updateFile.writeText(rawJson)
                        repository.refreshContent()
                        _updateEvents.emit(UpdateEvent.Success(validatedData.version))
                        Log.d(TAG, "Successfully updated to version ${validatedData.version}")
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download update", e)
                _updateEvents.emit(UpdateEvent.Error(e.localizedMessage ?: "Unknown error"))
                false
            }
        }
    }

    private fun extractImageNames(data: CourseData): Set<String> {
        val names = mutableSetOf<String>()
        data.units.forEach { unit ->
            unit.modules.forEach { module ->
                module.lessons.forEach { lesson ->
                    lesson.flashcards.forEach { card ->
                        card.imageName?.let { if (it.isNotBlank()) names.add(it) }
                    }
                }
            }
        }
        return names
    }

    private suspend fun downloadMissingImages(imageNames: Set<String>) {
        val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
        
        imageNames.forEach { name ->
            val imageFile = File(imagesDir, "$name.png")
            // Also check assets to avoid redundant downloads
            val assetExists = try {
                context.assets.open("images/$name.png").use { true }
            } catch (e: Exception) {
                false
            }

            if (!imageFile.exists() && !assetExists) {
                Log.d(TAG, "Downloading missing image: $name")
                downloadImage(name, imageFile)
            }
        }
    }

    private suspend fun downloadImage(name: String, destination: File) {
        try {
            val request = Request.Builder()
                .url("$IMAGES_BASE_URL/$name.png")
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Successfully downloaded image: $name")
                } else {
                    Log.e(TAG, "Failed to download image $name: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: $name", e)
        }
    }
}

sealed class UpdateEvent {
    data class Success(val version: Int) : UpdateEvent()
    data class Error(val message: String) : UpdateEvent()
}

@kotlinx.serialization.Serializable
data class VersionMetadata(
    val latest_content_version: Int
)
