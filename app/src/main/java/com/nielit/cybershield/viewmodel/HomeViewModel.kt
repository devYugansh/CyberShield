package com.nielit.cybershield.viewmodel

import androidx.compose.material3.DrawerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nielit.cybershield.domain.model.Module
import com.nielit.cybershield.domain.model.Lesson
import com.nielit.cybershield.domain.model.User
import com.nielit.cybershield.data.repository.ContentRepository
import com.nielit.cybershield.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val units: List<com.nielit.cybershield.domain.model.CourseUnit>,
        val lessonCompletionMap: Map<String, Boolean>,
        val overallProgress: Float
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    // State for the UI
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Dark mode state mirrored from ThemeRepository
    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Current user (mocked for now, integrate with Auth repository if available)
    val currentUser: User? = User("+91 98765 43210", isGuest = false)

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            combine(
                flowOf(contentRepository.units),
                contentRepository.completedLessonsFlow()
            ) { dataUnits, completedIds ->
                try {
                    val domainUnits = dataUnits.map { du ->
                        com.nielit.cybershield.domain.model.CourseUnit(
                            id = du.id,
                            title = du.title,
                            description = du.description,
                            modules = du.modules.map { dm ->
                                Module(
                                    id = dm.id,
                                    title = dm.title,
                                    description = dm.description,
                                    isPro = dm.isPro,
                                    lessons = dm.lessons.map { dl ->
                                        Lesson(
                                            id = dl.id,
                                            moduleId = dm.id,
                                            title = dl.title
                                        )
                                    }
                                )
                            }
                        )
                    }

                    val completionMap = mutableMapOf<String, Boolean>()
                    var totalLessons = 0
                    var completedCount = 0

                    domainUnits.forEach { unit ->
                        unit.modules.forEach { module ->
                            module.lessons.forEach { lesson ->
                                totalLessons++
                                val isComplete = completedIds.contains(lesson.id)
                                completionMap[lesson.id] = isComplete
                                if (isComplete) completedCount++
                            }
                        }
                    }

                    val progress = if (totalLessons == 0) 0f else (completedCount.toFloat() / totalLessons) * 100f

                    HomeUiState.Success(
                        units = domainUnits,
                        lessonCompletionMap = completionMap,
                        overallProgress = progress
                    )
                } catch (e: Exception) {
                    HomeUiState.Error("Data conversion error: ${e.message}")
                }
            }.collect {
                _uiState.value = it
            }
        }
    }

    // ── User Actions ──────────────────────────────────────────────────────

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkTheme(enabled)
        }
    }

    fun openDrawer(drawerState: DrawerState) {
        viewModelScope.launch {
            drawerState.open()
        }
    }

    fun closeDrawer(drawerState: DrawerState) {
        viewModelScope.launch {
            drawerState.close()
        }
    }

    fun signOut() {
        // Implement real sign out logic here
    }

    fun rateUs() {}
    fun followUs() {}
    fun moreCourses() {}
}
