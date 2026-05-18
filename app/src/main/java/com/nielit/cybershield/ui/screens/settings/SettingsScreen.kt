package com.nielit.cybershield.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.nielit.cybershield.ui.components.*
import com.nielit.cybershield.ui.theme.*
import com.nielit.cybershield.viewmodel.SettingsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    val user by viewModel.currentUser.collectAsState()

    SettingsContent(
        notifEnabled  = notifEnabled,
        isDarkMode    = isDarkMode,
        user          = user,
        maskedPhone   = maskedPhone,
        snackbarState = snackbarState,
        onNotifToggle = viewModel::setNotifEnabled,
        onDarkToggle  = viewModel::setDarkMode,
        onUpdateProfile = viewModel::updateProfile,
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
    user          : com.nielit.cybershield.domain.model.User?,
    maskedPhone   : String,
    snackbarState : SnackbarHostState,
    onNotifToggle : (Boolean) -> Unit,
    onDarkToggle  : (Boolean) -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut     : () -> Unit,
    modifier      : Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

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
        containerColor = MaterialTheme.colorScheme.background
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
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

                    HorizontalDivider(color = Border, thickness = 1.dp,
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
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation= CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SectionHeader(text = "Account", modifier = Modifier.weight(1f))
                        if (user != null && !user.isGuest) {
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Text("Edit")
                            }
                        }
                    }

                    // Account info
                    if (user != null && !user.name.isNullOrBlank()) {
                        InfoRow(label = "Name", value = user.name)
                        HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    
                    if (user != null && !user.email.isNullOrBlank()) {
                        InfoRow(label = "Email", value = user.email)
                        HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    if (user != null && !user.dob.isNullOrBlank()) {
                        InfoRow(label = "Date of Birth", value = user.dob)
                        HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Phone number – read only
                    if (maskedPhone.isNotEmpty()) {
                        InfoRow(label = "Phone Number", value = "+91 $maskedPhone")
                    } else if (user?.isGuest == true) {
                        InfoRow(label = "Mode", value = "Guest Access")
                    }

                    HorizontalDivider(color = Border, thickness = 1.dp,
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

    if (showEditProfileDialog && user != null) {
        EditProfileDialog(
            initialName = user.name ?: "",
            initialEmail = user.email ?: "",
            initialDob = user.dob ?: "",
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, email, dob ->
                onUpdateProfile(name, dob, email)
                showEditProfileDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    initialDob: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var dob by remember { mutableStateOf(initialDob) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(initialDob, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            null
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    CsTopBar(
                        title = "Edit Profile",
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = { onConfirm(name, email, dob) },
                                enabled = name.isNotBlank() && email.isNotBlank() && dob.isNotBlank()
                            ) {
                                Text("SAVE", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { },
                        label = { Text("Date of Birth") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "This information is used to personalize your learning journey and will be stored on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        dob = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
