package com.nielit.cybershield.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nielit.cybershield.data.model.Lesson
import com.nielit.cybershield.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FlashcardUiState {
    object Loading : FlashcardUiState()
    object Empty : FlashcardUiState()
    data class Error(val message: String) : FlashcardUiState()
    data class Success(
        val lesson: Lesson,
        val resumeCardIndex: Int = 0,
        val nextLessonId: String? = null
    ) : FlashcardUiState()
}

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Loading)
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var currentModuleId: String? = null
    private var currentLessonId: String? = null

    fun loadLesson(moduleId: String, lessonId: String) {
        currentModuleId = moduleId
        currentLessonId = lessonId
        
        viewModelScope.launch {
            _uiState.value = FlashcardUiState.Loading
            
            val module = contentRepository.moduleById(moduleId)
            if (module == null) {
                _uiState.value = FlashcardUiState.Error("Module not found")
                return@launch
            }
            
            val lesson = module.lessons.find { it.id == lessonId }
            if (lesson == null) {
                _uiState.value = FlashcardUiState.Error("Lesson not found")
                return@launch
            }

            val lessonIndex = module.lessons.indexOf(lesson)
            val nextLessonId = if (lessonIndex < module.lessons.lastIndex) {
                module.lessons[lessonIndex + 1].id
            } else null

            _uiState.value = FlashcardUiState.Success(
                lesson = lesson,
                resumeCardIndex = 0,
                nextLessonId = nextLessonId
            )
        }
    }

    fun onCardViewed(index: Int) {
        val currentState = _uiState.value
        if (currentState is FlashcardUiState.Success) {
            _uiState.value = currentState.copy(resumeCardIndex = index)
        }
    }

    fun onQuizAnswered(isCorrect: Boolean) {
        if (isCorrect) {
            val moduleId = currentModuleId ?: return
            val lessonId = currentLessonId ?: return
            viewModelScope.launch {
                contentRepository.markLessonComplete(moduleId, lessonId)
            }
        }
    }
}
