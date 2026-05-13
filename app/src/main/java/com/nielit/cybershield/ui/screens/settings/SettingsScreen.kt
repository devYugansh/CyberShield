package com.nielit.cybershield.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nielit.cybershield.ui.components.*
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN-08: SettingsScreen  –  stateful
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut     : () -> Unit,
    viewModel     : SettingsViewModel = hiltViewModel()
) {
    val notifEnabled  by viewModel.notifEnabled.collectAsState()
    val isDarkMode    by viewModel.isDarkMode.collectAsState()
    val maskedPhone   by viewModel.maskedPhone.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage.collect { msg ->
            snackbarState.showSnackbar(msg)
        }
    }

    SettingsContent(
        notifEnabled  = notifEnabled,
        isDarkMode    = isDarkMode,
        maskedPhone   = maskedPhone,
        snackbarState = snackbarState,
        onNotifToggle = viewModel::setNotifEnabled,
        onDarkToggle  = viewModel::setDarkMode,
        onNavigateBack= onNavigateBack,
        onSignOut     = {
            viewModel.signOut()
            onSignOut()
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsContent(
    notifEnabled  : Boolean,
    isDarkMode    : Boolean,
    maskedPhone   : String,
    snackbarState : SnackbarHostState,
    onNotifToggle : (Boolean) -> Unit,
    onDarkToggle  : (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut     : () -> Unit,
    modifier      : Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CsTopBar(
                title            = "Settings",
                navigationIcon   = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) },
        containerColor = Surface
    ) { padding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(Modifier.height(8.dp))

            // ── Preferences section ───────────────────────────────────────
            Card(
                shape    = MaterialTheme.shapes.medium,
                colors   = CardDefaults.cardColors(containerColor = White),
                elevation= CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SectionHeader(text = "Preferences")

                    ToggleRow(
                        label    = "Notifications",
                        sublabel = "Daily learning reminders",
                        checked  = notifEnabled,
                        onChecked= onNotifToggle
                    )

                    Divider(color = Border, thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp))

                    ToggleRow(
                        label    = "Dark Mode",
                        sublabel = "Override system theme",
                        checked  = isDarkMode,
                        onChecked= onDarkToggle
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Account section ───────────────────────────────────────────
            Card(
                shape    = MaterialTheme.shapes.medium,
                colors   = CardDefaults.cardColors(containerColor = White),
                elevation= CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SectionHeader(text = "Account")

                    // Phone number – read only
                    InfoRow(label = "Phone Number", value = "+91 $maskedPhone")

                    Divider(color = Border, thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp))

                    // Sign out
                    TextButton(
                        onClick  = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text  = "Sign Out",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ErrorRed
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Version text pinned bottom
            Text(
                text      = "CyberShield v1.0 — NIELIT",
                style     = MaterialTheme.typography.labelSmall,
                color     = MutedText,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
    }

    // Sign-out confirmation dialog
    if (showSignOutDialog) {
        ConfirmationDialog(
            title       = "Sign out?",
            message     = "Your local progress is saved.",
            confirmText = "Sign Out",
            dismissText = "Cancel",
            onConfirm   = { showSignOutDialog = false; onSignOut() },
            onDismiss   = { showSignOutDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoRow  –  read-only label / value pair
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(
    label   : String,
    value   : String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.titleMedium,
            color    = Navy,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
    }
}
