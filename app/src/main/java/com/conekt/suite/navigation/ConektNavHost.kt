package com.conekt.suite.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.conekt.suite.feature.auth.AuthScreen
import com.conekt.suite.feature.auth.PhoneSetupScreen
import com.conekt.suite.feature.canvas.CanvasScreen
import com.conekt.suite.feature.chat.ChatListScreen
import com.conekt.suite.feature.chat.ChatThreadScreen
import com.conekt.suite.feature.chat.UserProfileScreen
import com.conekt.suite.feature.library.MusicScreen
import com.conekt.suite.feature.library.MusicViewModel
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
    navController:    NavHostController,
    startDestination: String,
    musicViewModel:   MusicViewModel,
    modifier:         Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(AppRoutes.AUTH) {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate(Routes.PULSE) {
                        popUpTo(AppRoutes.AUTH) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(Routes.PHONE_SETUP) {
                        popUpTo(AppRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PHONE_SETUP) {
            PhoneSetupScreen(onComplete = {
                navController.navigate(Routes.PULSE) {
                    popUpTo(Routes.PHONE_SETUP) { inclusive = true }
                }
            })
        }

        // ── Pulse (Home) ──────────────────────────────────────────────────────
        composable(Routes.PULSE) {
            PulseScreen(
                onCreatePostClick = { navController.navigate(Routes.CREATE_POST) },
                onOpenChat        = { navController.navigate(Routes.CHAT) },
                onOpenUserProfile = { userId ->
                    navController.navigate(Routes.userProfile(userId))
                }
            )
        }

        composable(Routes.CREATE_POST) {
            CreatePostScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        // ── Chat list ─────────────────────────────────────────────────────────
        composable(Routes.CHAT) {
            ChatListScreen(
                onOpenThread  = { convId, otherId, name, avatar ->
                    navController.navigate(Routes.chatThread(convId, otherId, name))
                },
                onOpenProfile = { userId ->
                    navController.navigate(Routes.userProfile(userId))
                }
            )
        }

        // ── Chat thread ───────────────────────────────────────────────────────
        composable(
            route     = Routes.CHAT_THREAD,
            arguments = listOf(
                navArgument("convId")  { type = NavType.StringType },
                navArgument("otherId") { type = NavType.StringType },
                navArgument("name")    { type = NavType.StringType }
            )
        ) { back ->
            val convId  = back.arguments?.getString("convId")  ?: ""
            val otherId = back.arguments?.getString("otherId") ?: ""
            val rawName = back.arguments?.getString("name")    ?: ""
            val name    = runCatching { java.net.URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            ChatThreadScreen(
                conversationId = convId,
                otherUserId    = otherId,
                otherName      = name,
                otherAvatarUrl = null,
                onBack         = { navController.popBackStack() },
                onOpenProfile  = { userId -> navController.navigate(Routes.userProfile(userId)) }
            )
        }

        // ── User profile (other users) ────────────────────────────────────────
        composable(
            route     = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId       = userId,
                onBack       = { navController.popBackStack() },
                onStartChat  = { convId, otherId, name, avatar ->
                    navController.navigate(Routes.chatThread(convId, otherId, name))
                }
            )
        }

        // ── Main screens ──────────────────────────────────────────────────────
        composable(Routes.VAULT)  { VaultScreen() }
        composable(Routes.CANVAS) { CanvasScreen() }
        composable(Routes.MUSIC)  { MusicScreen(viewModel = musicViewModel) }

        composable(Routes.PROFILE) {
            ProfileScreen(onEditClick = { navController.navigate(Routes.EDIT_PROFILE) })
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}