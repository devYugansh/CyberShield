package com.nielit.cybershield.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nielit.cybershield.ui.screens.flashcard.FlashcardViewerScreen
import com.nielit.cybershield.ui.screens.home.HomeScreen
import com.nielit.cybershield.ui.screens.login.LoginScreen
import com.nielit.cybershield.ui.screens.otp.OtpVerifyScreen
import com.nielit.cybershield.ui.screens.settings.SettingsScreen
import com.nielit.cybershield.ui.screens.splash.SplashScreen

// ── Route definitions ─────────────────────────────────────────────────────────

object Routes {
    const val SPLASH   = "splash"
    const val LOGIN    = "login"
    const val OTP      = "otp/{verificationId}"
    const val HOME     = "home"
    const val FLASHCARD= "flashcard/{moduleId}/{lessonId}"
    const val SETTINGS = "settings"

    fun otp(verificationId: String)                      = "otp/$verificationId"
    fun flashcard(moduleId: String, lessonId: String)    = "flashcard/$moduleId/$lessonId"
}

// ── NavHost ───────────────────────────────────────────────────────────────────

@Composable
fun CyberShieldNavHost(
    navController : NavHostController,
    startDestination: String = Routes.SPLASH
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome  = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onOtpSent = { verificationId ->
                    navController.navigate(Routes.otp(verificationId))
                }
            )
        }

        composable(
            route     = Routes.OTP,
            arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
        ) { backStack ->
            val verificationId = backStack.arguments?.getString("verificationId").orEmpty()
            OtpVerifyScreen(
                verificationId = verificationId,
                onVerified     = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToLesson   = { moduleId, lessonId ->
                    navController.navigate(Routes.flashcard(moduleId, lessonId))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onSignOut            = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Routes.FLASHCARD,
            arguments = listOf(
                navArgument("moduleId")  { type = NavType.StringType },
                navArgument("lessonId")  { type = NavType.StringType }
            )
        ) { backStack ->
            val moduleId = backStack.arguments?.getString("moduleId").orEmpty()
            val lessonId = backStack.arguments?.getString("lessonId").orEmpty()
            FlashcardViewerScreen(
                moduleId      = moduleId,
                lessonId      = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onNextLesson   = { nextLessonId ->
                    navController.navigate(Routes.flashcard(moduleId, nextLessonId)) {
                        popUpTo(Routes.flashcard(moduleId, lessonId)) { inclusive = true }
                    }
                },
                onBackToModule = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut      = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
