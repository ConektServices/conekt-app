package com.conekt.suite

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.conekt.suite.core.session.SessionUiState
import com.conekt.suite.core.session.SessionViewModel
import com.conekt.suite.feature.library.MiniPlayer
import com.conekt.suite.feature.library.MusicViewModel
import com.conekt.suite.navigation.AppRoutes
import com.conekt.suite.navigation.ConektNavHost
import com.conekt.suite.navigation.Routes
import com.conekt.suite.ui.components.ConektBottomBar
import com.conekt.suite.ui.components.ConektSideMenu
import com.conekt.suite.ui.components.SplashScreen
import com.conekt.suite.ui.theme.ConektTheme

private val noBottomBarRoutes = setOf(
    AppRoutes.AUTH,
    Routes.CHAT_THREAD,   // full-screen thread hides bottom bar
    Routes.USER_PROFILE   // viewing another user's profile
)

class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    // AndroidViewModel requires AndroidViewModelFactory — the default factory
    // (ViewModelProvider.NewInstanceFactory) cannot instantiate AndroidViewModel
    // and will throw a RuntimeException ("Cannot create an instance of class…").
    private val musicViewModel: MusicViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            ConektTheme {
                val sessionState by sessionViewModel.uiState.collectAsState()
                ConektApp(sessionState, musicViewModel)
            }
        }
    }
}

@Composable
private fun ConektApp(sessionState: SessionUiState, musicViewModel: MusicViewModel) {
    if (sessionState.isLoading) { SplashScreen(); return }

    val startDestination = if (sessionState.isSignedIn) Routes.PULSE else AppRoutes.AUTH

    val navController     = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route

    var sideMenuOpen by rememberSaveable { mutableStateOf(false) }

    val showBottomBar = currentRoute != null && currentRoute !in noBottomBarRoutes
    val musicState    by musicViewModel.uiState.collectAsState()

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        ConektNavHost(
            navController    = navController,
            startDestination = startDestination,
            musicViewModel   = musicViewModel,
            modifier         = Modifier.fillMaxSize()
        )

        if (showBottomBar) {
            ConektSideMenu(
                visible      = sideMenuOpen,
                currentRoute = currentRoute,
                onDismiss    = { sideMenuOpen = false },
                onNavigate   = { route -> sideMenuOpen = false; navigateTo(route) }
            )

            // Global mini player — shown everywhere except the Music screen
            val isOnMusicScreen = currentRoute == Routes.MUSIC
            AnimatedVisibility(
                visible  = musicState.activeTrack != null && !isOnMusicScreen,
                enter    = slideInVertically { it } + fadeIn(),
                exit     = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 82.dp)
            ) {
                musicState.activeTrack?.let { track ->
                    MiniPlayer(
                        track    = track,
                        isPlaying = musicState.isPlaying,
                        progress  = musicState.progressFraction,
                        onExpand  = { navigateTo(Routes.MUSIC) },
                        onToggle  = musicViewModel::togglePlayPause,
                        onNext    = { musicViewModel.skipNext(musicViewModel.buildQueue()) }
                    )
                }
            }

            ConektBottomBar(
                currentRoute  = currentRoute,
                onTabSelected = { route -> navigateTo(route) },
                onMenuClick   = { sideMenuOpen = true },
                modifier      = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            )
        }
    }
}