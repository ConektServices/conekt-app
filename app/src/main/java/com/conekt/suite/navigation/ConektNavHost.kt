package com.conekt.suite.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.conekt.suite.feature.auth.AuthScreen
import com.conekt.suite.feature.auth.PhoneSetupScreen
import com.conekt.suite.feature.canvas.CanvasScreen
import com.conekt.suite.feature.library.MusicScreen
import com.conekt.suite.feature.profile.EditProfileScreen
import com.conekt.suite.feature.profile.ProfileScreen
import com.conekt.suite.feature.pulse.CreatePostScreen
import com.conekt.suite.feature.pulse.PulseScreen
import com.conekt.suite.feature.vault.VaultScreen

object AppRoutes {
    const val AUTH = "auth"
}

@Composable
fun ConektNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {

        composable(AppRoutes.AUTH) {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate(Routes.PULSE) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                },
                onSignUpSuccess = {
                    navController.navigate(Routes.PHONE_SETUP) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                }
            )
        }

        composable(Routes.PHONE_SETUP) {
            PhoneSetupScreen(
                onComplete = {
                    navController.navigate(Routes.PULSE) { popUpTo(Routes.PHONE_SETUP) { inclusive = true } }
                }
            )
        }

        composable(Routes.PULSE) {
            PulseScreen(
                onCreatePostClick = { navController.navigate(Routes.CREATE_POST) }
            )
        }

        composable(Routes.CREATE_POST) {
            CreatePostScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack()
                    // Optionally refresh pulse feed — PulseViewModel auto-refreshes on recompose
                }
            )
        }

        composable(Routes.VAULT)  { VaultScreen() }
        composable(Routes.CANVAS) { CanvasScreen() }
        composable(Routes.MUSIC)  { MusicScreen() }

        composable(Routes.PROFILE) {
            ProfileScreen(onEditClick = { navController.navigate(Routes.EDIT_PROFILE) })
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}