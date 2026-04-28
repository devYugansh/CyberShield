package com.nielit.cybershield.ui.screens.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.nielit.cybershield.data.model.Flashcard
import com.nielit.cybershield.data.model.Lesson
import com.nielit.cybershield.data.model.QuizQuestion
import com.nielit.cybershield.ui.components.*
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.FlashcardUiState
import com.nielit.cybershield.viewmodel.FlashcardViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-05 + 06: FlashcardViewerScreen  –  stateful
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FlashcardViewerScreen(
    moduleId      : String,
    lessonId      : String,
    onNavigateBack: () -> Unit,
    onNextLesson  : (String) -> Unit,
    onBackToModule: () -> Unit,
    viewModel     : FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(moduleId, lessonId) { viewModel.loadLesson(moduleId, lessonId) }

    when (val state = uiState) {
        is FlashcardUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
        }
        is FlashcardUiState.Error -> {
            FlashcardErrorState(state.message, onNavigateBack)
        }
        is FlashcardUiState.Empty -> {
            FlashcardEmptyState(onNavigateBack)
        }
        is FlashcardUiState.Success -> {
            FlashcardViewerContent(
                lesson         = state.lesson,
                resumeIndex    = state.resumeCardIndex,
                onBack         = onNavigateBack,
                onCardViewed   = viewModel::onCardViewed,
                onQuizAnswered = viewModel::onQuizAnswered,
                onNextLesson   = { onNextLesson(state.nextLessonId ?: lessonId) },
                onBackToModule = onBackToModule,
                hasNextLesson  = state.nextLessonId != null
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPagerApi::class)
@Composable
fun FlashcardViewerContent(
    lesson        : Lesson,
    resumeIndex   : Int,
    onBack        : () -> Unit,
    onCardViewed  : (Int) -> Unit,
    onQuizAnswered: (Boolean) -> Unit,
    onNextLesson  : () -> Unit,
    onBackToModule: () -> Unit,
    hasNextLesson : Boolean,
    modifier      : Modifier = Modifier
) {
    val flashcards   = lesson.flashcards
    val totalPages   = flashcards.size + 1  // +1 for quiz card
    val pagerState   = rememberPagerState(initialPage = resumeIndex.coerceAtMost(totalPages - 1))
    val scope        = rememberCoroutineScope()
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Quiz state (lifted here so we can share with NavigationRow)
    var selectedOption   by remember { mutableStateOf<Int?>(null) }
    var quizSubmitted    by remember { mutableStateOf(false) }
    var quizCorrect      by remember { mutableStateOf(false) }

    // Persist card index on pager change
    LaunchedEffect(pagerState.currentPage) { onCardViewed(pagerState.currentPage) }

    val isOnQuizCard   = pagerState.currentPage == totalPages - 1
    val isOnLastContent= pagerState.currentPage == flashcards.size - 1

    Scaffold(
        topBar = {
            CsTopBar(
                title = "Card ${pagerState.currentPage + 1} of $totalPages",
                navigationIcon = {
                    IconButton(onClick = {
                        if (pagerState.currentPage > 0) showLeaveDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* trigger native share */ }) {
                        Icon(Icons.Default.Share, "Share", tint = White)
                    }
                }
            )
        },
        containerColor = Surface
    ) { padding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress dash bar
            Spacer(Modifier.height(8.dp))
            ProgressDashBar(total = totalPages, current = pagerState.currentPage)
            Spacer(Modifier.height(8.dp))

            // Pager
            HorizontalPager(
                count    = totalPages,
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < flashcards.size) {
                    FlashCard(
                        card     = flashcards[page],
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    // SCREEN-06: Quiz card
                    lesson.quiz?.let { quiz ->
                        QuizCard(
                            quiz           = quiz,
                            selectedOption = selectedOption,
                            isSubmitted    = quizSubmitted,
                            isCorrect      = quizCorrect,
                            onOptionSelect = { if (!quizSubmitted) selectedOption = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } ?: QuizUnavailableCard(
                        onMarkComplete = { onQuizAnswered(false); onBackToModule() }
                    )
                }
            }

            // Navigation row
            NavigationRow(
                isOnQuizCard   = isOnQuizCard,
                isOnLastContent= isOnLastContent,
                quizSubmitted  = quizSubmitted,
                selectedOption = selectedOption,
                hasNextLesson  = hasNextLesson,
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onCheckResult = {
                    val correct = selectedOption == lesson.quiz?.correctIndex
                    quizCorrect  = correct
                    quizSubmitted= true
                    onQuizAnswered(correct)
                },
                onNextLesson   = onNextLesson,
                onBackToModule = onBackToModule,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Leave lesson dialog
    if (showLeaveDialog) {
        ConfirmationDialog(
            title       = "Leave lesson?",
            message     = "Your progress is saved. You can resume later.",
            confirmText = "Leave",
            dismissText = "Stay",
            onConfirm   = { showLeaveDialog = false; onBack() },
            onDismiss   = { showLeaveDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FlashCard  –  individual content card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FlashCard(
    card    : Flashcard,
    modifier: Modifier = Modifier
) {
    Card(
        shape    = MaterialTheme.shapes.medium,
        colors   = CardDefaults.cardColors(containerColor = White),
        elevation= CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Optional illustration (16:9)
            card.imageRes?.let {
                AsyncImage(
                    model             = it,
                    contentDescription= null,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text  = card.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    ),
                    color = Navy
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = card.body,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-06: QuizCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuizCard(
    quiz          : QuizQuestion,
    selectedOption: Int?,
    isSubmitted   : Boolean,
    isCorrect     : Boolean,
    onOptionSelect: (Int) -> Unit,
    modifier      : Modifier = Modifier
) {
    Card(
        shape    = MaterialTheme.shapes.medium,
        colors   = CardDefaults.cardColors(containerColor = White),
        elevation= CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // QUESTION label
            Text(
                text  = "QUESTION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color         = MutedText,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(Modifier.height(8.dp))

            // Question body
            Text(
                text  = quiz.question,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = Navy
            )

            Spacer(Modifier.height(20.dp))

            // Option rows
            quiz.options.forEachIndexed { index, option ->
                OptionRow(
                    text           = option,
                    state          = when {
                        !isSubmitted && selectedOption == index -> OptionState.SELECTED
                        isSubmitted && index == quiz.correctIndex -> OptionState.CORRECT
                        isSubmitted && selectedOption == index && index != quiz.correctIndex -> OptionState.WRONG
                        else -> OptionState.DEFAULT
                    },
                    onClick        = { onOptionSelect(index) },
                    modifier       = Modifier.padding(vertical = 6.dp)
                )
            }

            // Explanation
            if (isSubmitted) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text  = if (isCorrect) "Correct!" else "Incorrect",
                    color = if (isCorrect) SuccessGreen else Color.Red,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = quiz.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Navy
                )
            }
        }
    }
}

@Composable
fun OptionRow(
    text    : String,
    state   : OptionState,
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when(state) {
        OptionState.SELECTED -> Blue.copy(alpha = 0.1f)
        OptionState.CORRECT  -> SuccessGreen.copy(alpha = 0.1f)
        OptionState.WRONG    -> Color.Red.copy(alpha = 0.1f)
        else                 -> White
    }
    val borderColor = when(state) {
        OptionState.SELECTED -> Blue
        OptionState.CORRECT  -> SuccessGreen
        OptionState.WRONG    -> Color.Red
        else                 -> Color(0xFFE0E0E0)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (state == OptionState.DEFAULT) Color(0xFFBDBDBD) else borderColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = if (state == OptionState.DEFAULT) Navy else borderColor
        )
    }
}

enum class OptionState { DEFAULT, SELECTED, CORRECT, WRONG }

// ─────────────────────────────────────────────────────────────────────────────
// Navigation row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NavigationRow(
    isOnQuizCard   : Boolean,
    isOnLastContent: Boolean,
    quizSubmitted  : Boolean,
    selectedOption : Int?,
    hasNextLesson  : Boolean,
    onNext         : () -> Unit,
    onCheckResult  : () -> Unit,
    onNextLesson   : () -> Unit,
    onBackToModule : () -> Unit,
    modifier       : Modifier = Modifier
) {
    val label: String
    val action: () -> Unit
    val enabled: Boolean

    when {
        isOnQuizCard && quizSubmitted -> {
            label   = if (hasNextLesson) "Next Lesson →" else "Back to Module"
            action  = if (hasNextLesson) onNextLesson else onBackToModule
            enabled = true
        }
        isOnQuizCard -> {
            label   = "Check Result"
            action  = onCheckResult
            enabled = selectedOption != null
        }
        isOnLastContent -> {
            label   = "Take Quiz"
            action  = onNext
            enabled = true
        }
        else -> {
            label   = "Next →"
            action  = onNext
            enabled = true
        }
    }

    CsPrimaryButton(text = label, onClick = action, enabled = enabled, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty / Error / Quiz-unavailable states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FlashcardEmptyState(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("This lesson has no content yet. Check back soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CsPrimaryButton("Back", onClick = onBack, modifier = Modifier.width(160.dp))
    }
}

@Composable
fun FlashcardErrorState(message: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MutedText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CsPrimaryButton("Back to Module", onClick = onBack, modifier = Modifier.width(200.dp))
    }
}

@Composable
fun QuizUnavailableCard(onMarkComplete: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("Quiz unavailable for this lesson.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CsPrimaryButton("Mark Complete Anyway", onClick = onMarkComplete,
            modifier = Modifier.fillMaxWidth())
    }
}
