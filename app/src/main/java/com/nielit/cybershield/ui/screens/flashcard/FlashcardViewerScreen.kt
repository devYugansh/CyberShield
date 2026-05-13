package com.nielit.cybershield.ui.screens.flashcard

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
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
    val context      = LocalContext.current
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Quiz state
    var selectedOption   by remember { mutableStateOf<Int?>(null) }
    var quizSubmitted    by remember { mutableStateOf(false) }
    var quizCorrect      by remember { mutableStateOf(false) }

    // Timer state for Next button delay
    var remainingSeconds by remember { mutableStateOf(2) }

    // Persist card index and reset timer on pager change
    LaunchedEffect(pagerState.currentPage) {
        onCardViewed(pagerState.currentPage)
        remainingSeconds = 2
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val isOnQuizCard   = pagerState.currentPage == totalPages - 1
    val isOnLastContent= pagerState.currentPage == flashcards.size - 1

    Scaffold(
        topBar = {
            CsTopBar(
                title = lesson.title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (pagerState.currentPage > 0) showLeaveDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.Default.Close, "Exit")
                    }
                    IconButton(onClick = {
                        val currentCard = if (!isOnQuizCard) flashcards[pagerState.currentPage] else null
                        val shareText = if (currentCard != null) {
                            "CyberShield Flashcard:\n\n${currentCard.title}\n\n${currentCard.body}"
                        } else {
                            "Check out this lesson on CyberShield: ${lesson.title}"
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
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
                .background(Surface)
        ) {
            // Enhanced Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = Navy.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Blue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $totalPages",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Blue,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ProgressDashBar(
                    total = totalPages,
                    current = pagerState.currentPage,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Pager with dynamic card sizing
            HorizontalPager(
                count    = totalPages,
                state    = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                itemSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (page < flashcards.size) {
                        FlashCard(
                            card     = flashcards[page],
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                        )
                    } else {
                        lesson.quiz?.let { quiz ->
                            QuizCard(
                                quiz           = quiz,
                                selectedOption = selectedOption,
                                isSubmitted    = quizSubmitted,
                                isCorrect      = quizCorrect,
                                onOptionSelect = { if (!quizSubmitted) selectedOption = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .shadow(8.dp, RoundedCornerShape(24.dp))
                            )
                        } ?: QuizUnavailableCard(
                            onMarkComplete = { onQuizAnswered(false); onBackToModule() }
                        )
                    }
                }
            }

            // Navigation row with delay countdown
            NavigationRow(
                isOnQuizCard    = isOnQuizCard,
                isOnLastContent = isOnLastContent,
                quizSubmitted   = quizSubmitted,
                selectedOption  = selectedOption,
                hasNextLesson   = hasNextLesson,
                remainingSeconds = remainingSeconds,
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
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth()
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
// FlashCard  –  individual content card (enhanced UI)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FlashCard(
    card    : Flashcard,
    modifier: Modifier = Modifier
) {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = White),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Image section - dynamic height based on presence
            card.imageRes?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                ) {
                    AsyncImage(
                        model             = it,
                        contentDescription= null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    // Gradient overlay for modern look
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                    startY = 300f
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Tag
                Surface(
                    color = Blue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "EXPLANATION",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Blue
                    )
                }

                Text(
                    text  = card.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    ),
                    color = Navy
                )

                // Divider
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Border.copy(alpha = 0.5f)
                )

                Text(
                    text  = card.body,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Navy.copy(alpha = 0.8f)
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-06: QuizCard (enhanced UI)
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
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = White),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QUESTION label
            Surface(
                color = Blue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "KNOWLEDGE CHECK",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Blue
                )
            }

            Text(
                text  = quiz.question,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                ),
                color = Navy
            )

            Spacer(Modifier.height(8.dp))

            quiz.options.forEachIndexed { index, option ->
                OptionRow(
                    text           = option,
                    state          = when {
                        !isSubmitted && selectedOption == index -> OptionState.SELECTED
                        isSubmitted && index == quiz.correctIndex -> OptionState.CORRECT
                        isSubmitted && selectedOption == index && index != quiz.correctIndex -> OptionState.WRONG
                        else -> OptionState.DEFAULT
                    },
                    onClick        = { onOptionSelect(index) }
                )
            }

            AnimatedVisibility(
                visible = isSubmitted,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            color = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isCorrect) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text  = if (isCorrect) "Excellent!" else "Not quite right",
                                color = if (isCorrect) SuccessGreen else ErrorRed,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text  = quiz.explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = Navy
                        )
                    }
                }
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
    val bgColor by animateColorAsState(
        when(state) {
            OptionState.SELECTED -> Blue.copy(alpha = 0.08f)
            OptionState.CORRECT  -> SuccessGreen.copy(alpha = 0.12f)
            OptionState.WRONG    -> ErrorRed.copy(alpha = 0.12f)
            else                 -> Surface
        }, label = "bgColor"
    )
    val borderColor by animateColorAsState(
        when(state) {
            OptionState.SELECTED -> Blue
            OptionState.CORRECT  -> SuccessGreen
            OptionState.WRONG    -> ErrorRed
            else                 -> Border
        }, label = "borderColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, borderColor, CircleShape)
                .padding(4.dp)
        ) {
            if (state != OptionState.DEFAULT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(borderColor)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (state != OptionState.DEFAULT) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (state != OptionState.DEFAULT) borderColor else Navy
        )
    }
}

enum class OptionState { DEFAULT, SELECTED, CORRECT, WRONG }

// ─────────────────────────────────────────────────────────────────────────────
// Navigation row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NavigationRow(
    isOnQuizCard    : Boolean,
    isOnLastContent : Boolean,
    quizSubmitted   : Boolean,
    selectedOption  : Int?,
    hasNextLesson   : Boolean,
    remainingSeconds: Int,
    onNext          : () -> Unit,
    onCheckResult   : () -> Unit,
    onNextLesson    : () -> Unit,
    onBackToModule  : () -> Unit,
    modifier        : Modifier = Modifier
) {
    val isTimerActive = remainingSeconds > 0 && !isOnQuizCard
    val label: String
    val action: () -> Unit
    val enabled: Boolean

    when {
        isOnQuizCard && quizSubmitted -> {
            label   = if (hasNextLesson) "Next Lesson" else "Finish Lesson"
            action  = if (hasNextLesson) onNextLesson else onBackToModule
            enabled = true
        }
        isOnQuizCard -> {
            label   = "Check Answer"
            action  = onCheckResult
            enabled = selectedOption != null
        }
        isOnLastContent -> {
            label   = if (isTimerActive) "Wait... ($remainingSeconds)" else "Start Quiz"
            action  = onNext
            enabled = !isTimerActive
        }
        else -> {
            label   = if (isTimerActive) "Wait... ($remainingSeconds)" else "Next Card"
            action  = onNext
            enabled = !isTimerActive
        }
    }

    CsPrimaryButton(
        text = label,
        onClick = action,
        enabled = enabled,
        modifier = modifier.animateContentSize()
    )
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
