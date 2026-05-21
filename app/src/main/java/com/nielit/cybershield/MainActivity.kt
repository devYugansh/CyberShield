package com.nielit.cybershield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.nielit.cybershield.data.remote.UpdateEvent
import com.nielit.cybershield.ui.components.ConfirmationDialog
import com.nielit.cybershield.navigation.CyberShieldNavHost
import androidx.navigation.compose.rememberNavController
import com.nielit.cybershield.navigation.Routes
import com.nielit.cybershield.ui.theme.CyberShieldTheme
import com.nielit.cybershield.viewmodel.SettingsViewModel
import com.nielit.cybershield.data.remote.ContentUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var contentUpdateManager: ContentUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            if (contentUpdateManager.checkForUpdates()) {
                // Potential: Show a toast or update HomeViewModel if necessary
            }
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            // Listen for background content updates
            LaunchedEffect(Unit) {
                contentUpdateManager.updateEvents.collect { event ->
                    when (event) {
                        is UpdateEvent.Success -> {
                            Toast.makeText(context, "New content available! Refreshing...", Toast.LENGTH_LONG).show()
                        }
                        is UpdateEvent.Error -> {
                            // Silently fail or log for now
                        }
                    }
                }
            }

            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            
            var showExitDialog by remember { mutableStateOf(false) }
            val activity = (LocalContext.current as? ComponentActivity)

            CyberShieldTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                // Exit confirmation logic
                BackHandler(enabled = true) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute == Routes.HOME || currentRoute == Routes.LOGIN) {
                        showExitDialog = true
                    } else {
                        navController.popBackStack()
                    }
                }

                if (showExitDialog) {
                    ConfirmationDialog(
                        title = "Exit App",
                        message = "Are you sure you want to exit CyberShield?",
                        confirmText = "Exit",
                        dismissText = "Cancel",
                        onConfirm = { activity?.finish() },
                        onDismiss = { showExitDialog = false }
                    )
                }

                CyberShieldNavHost(navController = navController)
            }
        }
    }
}