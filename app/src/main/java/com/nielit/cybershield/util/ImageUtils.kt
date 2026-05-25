package com.nielit.cybershield.util

import android.content.Context
import java.io.File

object ImageUtils {
    /**
     * Resolves the image source for a given image name.
     * Returns a [File] if the image exists in internal storage (OTA update),
     * otherwise returns the resource ID from drawables if it exists,
     * or null if not found.
     */
    fun getImageSource(context: Context, imageName: String?): Any? {
        if (imageName.isNullOrBlank()) return null

        // 1. Check internal storage (OTA images)
        val otaImageFile = File(context.filesDir, "images/$imageName.png")
        if (otaImageFile.exists()) {
            return otaImageFile
        }

        // 2. Fallback to bundled drawable resources
        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        if (resId != 0) {
            return resId
        }

        return null
    }
}
