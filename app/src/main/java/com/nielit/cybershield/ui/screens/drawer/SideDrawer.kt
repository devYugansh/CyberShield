package com.nielit.cybershield.ui.screens.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nielit.cybershield.domain.model.User
import com.nielit.cybershield.ui.components.AvatarCircle
import com.nielit.cybershield.ui.components.ConfirmationDialog
import com.nielit.cybershield.ui.components.DrawerNavItem
import com.nielit.cybershield.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-07: SideDrawer  –  DrawerContent slot for ModalNavigationDrawer
// Called from HomeScreen; stateless – all events bubble up.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SideDrawer(
    user             : User?,
    isDarkMode       : Boolean,
    onDarkModeToggle : (Boolean) -> Unit,
    onNavigateSettings: () -> Unit,
    onSignOut        : () -> Unit,
    onRateUs         : () -> Unit,
    onFollowUs       : () -> Unit,
    onMoreCourses    : () -> Unit,
    modifier         : Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Drawer occupies ~70% screen width
    ModalDrawerSheet(
        modifier      = modifier.fillMaxHeight(),
        drawerContainerColor = White,
        drawerShape   = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .statusBarsPadding()
        ) {

            // ── Header – avatar + user info ──────────────────────────────────
            DrawerHeader(user = user)

            HorizontalDivider(color = Border, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // ── Navigation items ─────────────────────────────────────────────
            DrawerNavItem(
                icon    = Icons.Default.Settings,
                label   = "Settings",
                onClick = onNavigateSettings
            )

            DrawerNavItem(
                icon    = Icons.Default.Star,
                label   = "Rate Us",
                onClick = onRateUs
            )

            DrawerNavItem(
                icon    = Icons.Default.People,
                label   = "Follow Us",
                onClick = onFollowUs
            )

            DrawerNavItem(
                icon    = Icons.Default.School,
                label   = "More Courses",
                onClick = onMoreCourses
            )

            // Dark Mode toggle – inline in drawer item
            DrawerNavItem(
                icon    = Icons.Default.DarkMode,
                label   = "Dark Mode",
                onClick = { onDarkModeToggle(!isDarkMode) },
                trailing = {
                    Switch(
                        checked         = isDarkMode,
                        onCheckedChange = onDarkModeToggle,
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = White,
                            checkedTrackColor   = Blue,
                            uncheckedTrackColor = Border
                        )
                    )
                }
            )

            HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            // ── Sign out ─────────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick  = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text  = "Sign Out",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ErrorRed
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Sign-out confirmation dialog
    if (showSignOutDialog) {
        ConfirmationDialog(
            title       = "Sign out?",
            message     = "Your progress is saved locally.",
            confirmText = "Sign Out",
            dismissText = "Cancel",
            onConfirm   = { showSignOutDialog = false; onSignOut() },
            onDismiss   = { showSignOutDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawerHeader  –  Avatar + name + masked phone
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerHeader(user: User?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        AvatarCircle(
            initials = user?.initials ?: "G",
            size     = 56.dp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text  = if (user == null || user.isGuest) "Hello, Guest"
                    else "Hello, +91 ${user.maskedPhone}",
            style = MaterialTheme.typography.titleLarge,
            color = Navy
        )
        if (user != null && !user.isGuest) {
            Spacer(Modifier.height(2.dp))
            Text(
                text  = user.maskedPhone,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}
