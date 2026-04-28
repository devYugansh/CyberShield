package com.nielit.cybershield.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nielit.cybershield.domain.model.Lesson
import com.nielit.cybershield.domain.model.Module
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

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            SideDrawer(
                user              = viewModel.currentUser,
                isDarkMode        = isDarkMode,
                onDarkModeToggle  = viewModel::toggleDarkMode,
                onNavigateSettings= {
                    viewModel.closeDrawer(drawerState)
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
            onHamburgerClick    = { viewModel.openDrawer(drawerState) },
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
                title            = "CyberShield",
                navigationIcon   = {
                    IconButton(onClick = onHamburgerClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = White)
                    }
                }
            )
        },
        containerColor = Surface
    ) { padding ->

        when (uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            }

            is HomeUiState.Error -> {
                HomeErrorState(message = uiState.message, onRetry = onRetry,
                    modifier = Modifier.padding(padding))
            }

            is HomeUiState.Success -> {
                HomeSuccessContent(
                    overallProgress = uiState.overallProgress,
                    modules         = uiState.modules,
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
    modules        : List<Module>,
    completedMap   : Map<String, Boolean>,  // lessonId -> isComplete
    onLessonClick  : (String, String) -> Unit,
    modifier       : Modifier = Modifier
) {
    // Track which module is expanded (accordion: only one at a time)
    var expandedModuleId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // ── Progress section ──
        item {
            ProgressSection(
                overallProgress = overallProgress,
                modifier        = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Module cards ──
        if (modules.isEmpty()) {
            item { EmptyFirstLaunchNudge() }
        } else {
            items(modules, key = { it.id }) { module ->
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

// ─────────────────────────────────────────────────────────────────────────────
// ProgressSection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressSection(
    overallProgress: Float,
    modifier       : Modifier = Modifier
) {
    Card(
        shape    = MaterialTheme.shapes.medium,
        colors   = CardDefaults.cardColors(containerColor = White),
        elevation= CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            ProgressRing(percentage = overallProgress)
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Overall Progress: ${overallProgress.toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Navy
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ModuleCard  –  Expandable card with lesson rows
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
        shape    = MaterialTheme.shapes.medium,
        colors   = CardDefaults.cardColors(containerColor = White),
        elevation= CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Card header (always visible)
            ModuleCardHeader(
                title          = module.title,
                completedCount = completedCount,
                totalCount     = module.lessons.size,
                isExpanded     = isExpanded,
                isPro          = module.isPro,
                onClick        = onHeaderClick
            )

            // Expandable lesson list
            AnimatedVisibility(
                visible       = isExpanded,
                enter         = expandVertically(animationSpec = tween(250)),
                exit          = shrinkVertically(animationSpec = tween(250))
            ) {
                Column {
                    Divider(color = Border, thickness = 1.dp)
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

@Composable
private fun ModuleCardHeader(
    title         : String,
    completedCount: Int,
    totalCount    : Int,
    isExpanded    : Boolean,
    isPro         : Boolean,
    onClick       : () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = Navy
                    )
                )
                if (isPro) {
                    Spacer(Modifier.width(8.dp))
                    ProBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "$completedCount/$totalCount completed",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                          else            Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MutedText
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LessonRow  –  Full-width tappable row, min 48dp touch target
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
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Status icon
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle
                          else             Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isCompleted) "Completed" else "Not started",
            tint   = if (isCompleted) SuccessGreen else MutedText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text  = lesson.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCompleted) MutedText else Navy,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProBadge chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Blue.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text  = "PRO",
            style = MaterialTheme.typography.labelMedium.copy(
                color      = Blue,
                fontWeight = FontWeight.Bold
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CsPrimaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(160.dp))
    }
}
