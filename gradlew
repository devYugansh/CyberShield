# Project-wide Gradle settings.
# https://docs.gradle.org/current/userguide/build_environment.html

# ── AndroidX ──────────────────────────────────────────────────────────────
# Required: enables AndroidX support. Without this, all androidx.* runtime
# dependencies cause build failures (the error you saw above).
android.useAndroidX=true

# Automatically migrates third-party libraries that use the old
# android.support.* namespace to use the equivalent AndroidX packages.
android.enableJetifier=true

# ── JVM heap ──────────────────────────────────────────────────────────────
# Kotlin + Compose annotation processing is memory-hungry.
# Bump if you hit OutOfMemoryError during kapt / KSP.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# ── Build optimisation ────────────────────────────────────────────────────
# Enable Gradle configuration cache and parallel builds for faster rebuilds.
org.gradle.parallel=true
org.gradle.caching=true

# ── Kotlin ────────────────────────────────────────────────────────────────
kotlin.code.style=official