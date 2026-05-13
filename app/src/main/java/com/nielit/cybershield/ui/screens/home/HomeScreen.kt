package com.nielit.cybershield.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.nielit.cybershield.domain.model.Lesson
import com.nielit.cybershield.domain.model.Module
import com.nielit.cybershield.domain.model.CourseUnit
import com.nielit.cybershield.ui.components.*
import com.nielit.cybershield.ui.screens.drawer.SideDrawer
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.HomeUiState
import com.nielit.cybershield.viewmodel.HomeViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-04: HomeScreen  –  stateful
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onNavigateToLesson  : (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignOut           : () -> Unit,
    viewModel           : HomeViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val isDarkMode    by viewModel.isDarkMode.collectAsState()
    val drawerState   = rememberDrawerState(DrawerValue.Closed)
    val scope         = rememberCoroutineScope()

    val currentUser   by viewModel.currentUser.collectAsState()

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            SideDrawer(
                user              = currentUser,
                isDarkMode        = isDarkMode,
                onDarkModeToggle  = viewModel::toggleDarkMode,
                onNavigateSettings= {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onSignOut         = {
                    viewModel.signOut()
                    onSignOut()
                },
                onRateUs          = viewModel::rateUs,
                onFollowUs        = viewModel::followUs,
                onMoreCourses     = viewModel::moreCourses
            )
        }
    ) {
        HomeContent(
            uiState             = uiState,
            onHamburgerClick    = { scope.launch { drawerState.open() } },
            onLessonClick       = onNavigateToLesson,
            onRetry             = viewModel::loadContent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeContent(
    uiState          : HomeUiState,
    onHamburgerClick : () -> Unit,
    onLessonClick    : (String, String) -> Unit,
    onRetry          : () -> Unit,
    modifier         : Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CsTopBar(
                title = "CyberShield",
                navigationIcon = {
                    IconButton(onClick = onHamburgerClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when (uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is HomeUiState.Error -> {
                HomeErrorState(message = uiState.message, onRetry = onRetry,
                    modifier = Modifier.padding(padding))
            }

            is HomeUiState.Success -> {
                HomeSuccessContent(
                    overallProgress = uiState.overallProgress,
                    units           = uiState.units,
                    completedMap    = uiState.lessonCompletionMap,
                    onLessonClick   = onLessonClick,
                    modifier        = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeSuccessContent(
    overallProgress: Float,
    units          : List<CourseUnit>,
    completedMap   : Map<String, Boolean>,  // lessonId -> isComplete
    onLessonClick  : (String, String) -> Unit,
    modifier       : Modifier = Modifier
) {
    // Track which module is expanded (accordion: only one at a time)
    var expandedModuleId by remember { mutableStateOf<String?>(null) }

    val completedCount = completedMap.count { it.value }
    val totalLessons = units.sumOf { it.modules.sumOf { m -> m.lessons.size } }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // ── Progress section ──
        item {
            ProgressSection(
                overallProgress = overallProgress,
                completedCount  = completedCount,
                totalLessons    = totalLessons,
                modifier        = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Units & Module cards ──
        if (units.isEmpty()) {
            item { EmptyFirstLaunchNudge() }
        } else {
            units.forEach { unit ->
                item(key = unit.id) {
                    UnitHeader(unit)
                }

                items(unit.modules, key = { it.id }) { module ->
                    val completedCount = module.lessons.count { completedMap[it.id] == true }
                    ModuleCard(
                        module         = module,
                        completedCount = completedCount,
                        isExpanded     = expandedModuleId == module.id,
                        completedMap   = completedMap,
                        onHeaderClick  = {
                            expandedModuleId = if (expandedModuleId == module.id) null else module.id
                        },
                        onLessonClick  = onLessonClick,
                        modifier       = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitHeader(unit: CourseUnit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp)
    ) {
        Text(
            text = unit.title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.2.sp
            )
        )
        if (unit.description.isNotEmpty()) {
            Text(
                text = unit.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProgressSection – Enhanced Government/NIELIT themed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressSection(
    overallProgress: Float,
    completedCount: Int,
    totalLessons: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation= CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "LEARNING DASHBOARD",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Progress Ring + Stats Container
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left: Progress Ring
                ProgressRing(
                    percentage = overallProgress,
                    size = 120.dp,
                    strokeWidth = 14.dp,
                    progressColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Right: Stats
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    StatItem(
                        label = "Lessons Completed",
                        value = "$completedCount/$totalLessons",
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    StatItem(
                        label = "Daily Goal",
                        value = "Achieved",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Motivational message
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“The only truly secure system is one that is powered off, cast in a block of concrete and sealed in a lead-lined room...”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = 22.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ModuleCard  –  Expandable card with lesson rows (Government themed)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModuleCard(
    module        : Module,
    completedCount: Int,
    isExpanded    : Boolean,
    completedMap  : Map<String, Boolean>,
    onHeaderClick : () -> Unit,
    onLessonClick : (String, String) -> Unit,
    modifier      : Modifier = Modifier
) {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation= CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column {
            // Card header (always visible) with Navy left accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = if (isExpanded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onHeaderClick)
                        .padding(0.dp)
                ) {
                    // Left accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text  = module.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                            )
                            if (module.isPro) {
                                Spacer(Modifier.width(8.dp))
                                ProBadge()
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        // Progress mini-bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            val progress = if (module.lessons.isEmpty()) 0f else (completedCount.toFloat() / module.lessons.size)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = "$completedCount/${module.lessons.size} lessons completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                      else            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )
                }
            }

            // Expandable lesson list
            AnimatedVisibility(
                visible       = isExpanded,
                enter         = expandVertically(animationSpec = tween(250)),
                exit          = shrinkVertically(animationSpec = tween(250))
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    module.lessons.forEach { lesson ->
                        LessonRow(
                            lesson      = lesson,
                            isCompleted = completedMap[lesson.id] == true,
                            onClick     = { onLessonClick(module.id, lesson.id) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LessonRow  –  Full-width tappable row, min 48dp touch target (Government styled)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LessonRow(
    lesson     : Lesson,
    isCompleted: Boolean,
    onClick    : () -> Unit,
    modifier   : Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = if (isCompleted) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        // Status icon
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle
                          else             Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isCompleted) "Completed" else "Not started",
            tint   = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text  = lesson.title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCompleted) 0.4f else 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProBadge chip (Government premium badge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text  = "🔐 ADVANCED",
            style = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty/Error states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyFirstLaunchNudge(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text  = "Start your first lesson below",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HomeErrorState(
    message : String,
    onRetry : () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MutedText,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CsPrimaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(160.dp))
    }
}
