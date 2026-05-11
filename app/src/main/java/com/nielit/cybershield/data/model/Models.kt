package com.nielit.cybershield.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Content models ────────────────────────────────────────────────────────────

@Serializable
data class CourseUnit(
    val id          : String,
    val title       : String,
    val description : String,
    val modules     : List<Module> = emptyList()
)

@Serializable
data class Module(
    val id          : String,
    val title       : String,
    val description : String = "",
    @SerialName("is_pro")
    val isPro       : Boolean = false,
    val lessons     : List<Lesson> = emptyList()
)

@Serializable
data class Lesson(
    val id          : String,
    val title       : String,
    @SerialName("cards")
    val flashcards  : List<Flashcard> = emptyList(),
    val quiz        : QuizQuestion? = null
)

@Serializable
data class Flashcard(
    val id          : String,
    val title       : String,
    val body        : String,
    val imageRes    : Int? = null       // nullable – optional illustration
)

@Serializable
data class QuizQuestion(
    val question    : String,
    val options     : List<String>,     // always 4 in V1
    @SerialName("correct_index")
    val correctIndex: Int,
    val explanation : String
)

// ── Progress models ───────────────────────────────────────────────────────────

data class LessonProgress(
    val lessonId        : String,
    val isCompleted     : Boolean = false,
    val resumeCardIndex : Int     = 0
)

data class ModuleProgress(
    val moduleId    : String,
    val completed   : Int,
    val total       : Int
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

// ── Auth model ────────────────────────────────────────────────────────────────

data class User(
    val phone       : String,           // e.g. "+919876543210"
    val isGuest     : Boolean = false
) {
    /** Masked form: +91 98765 XXXXX */
    val maskedPhone: String get() {
        if (phone.length < 6) return phone
        val last = phone.takeLast(5)
        return "${phone.take(phone.length - 5)}${last.replace(Regex("."), "X")}"
    }

    /** Initials shown in AvatarCircle */
    val initials: String get() = if (isGuest) "G" else phone.takeLast(2)
}
