package com.conekt.suite

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.conekt.suite.core.session.SessionUiState
import com.conekt.suite.core.session.SessionViewModel
import com.conekt.suite.navigation.AppRoutes
import com.conekt.suite.navigation.ConektNavHost
import com.conekt.suite.navigation.Routes
import com.conekt.suite.ui.components.ConektBottomBar
import com.conekt.suite.ui.components.ConektSideMenu
import com.conekt.suite.ui.components.SplashScreen
import com.conekt.suite.ui.theme.ConektTheme

// Routes where the bottom bar should NOT appear
private val noBottomBarRoutes = setOf(AppRoutes.AUTH)

class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            ConektTheme {
                val sessionState by sessionViewModel.uiState.collectAsState()
                ConektApp(sessionState)
            }
        }
    }
}

@Composable
private fun ConektApp(sessionState: SessionUiState) {

    // While Supabase is re-hydrating the stored session, show nothing (splash)
    if (sessionState.isLoading) {
        SplashScreen()
        return
    }

    val startDestination = if (sessionState.isSignedIn) Routes.PULSE else AppRoutes.AUTH

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var sideMenuOpen by rememberSaveable { mutableStateOf(false) }

    val showBottomBar = currentRoute != null && currentRoute !in noBottomBarRoutes

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConektNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        )

        if (showBottomBar) {
            ConektSideMenu(
                visible = sideMenuOpen,
                currentRoute = currentRoute,
                onDismiss = { sideMenuOpen = false },
                onNavigate = { route ->
                    sideMenuOpen = false
                    navigateTo(route)
                }
            )

            ConektBottomBar(
                currentRoute = currentRoute,
                onTabSelected = { route -> navigateTo(route) },
                onMenuClick = { sideMenuOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            )
        }
    }
}