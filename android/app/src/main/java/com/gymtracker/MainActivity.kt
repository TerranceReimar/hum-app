package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.UiEvent
import com.gymtracker.ui.history.HistoryScreen
import com.gymtracker.ui.home.HomeScreen
import com.gymtracker.ui.manage.ManageScreen
import com.gymtracker.ui.settings.SettingsScreen
import com.gymtracker.ui.theme.GymTrackerTheme
import com.gymtracker.ui.theme.Neon
import com.gymtracker.ui.theme.SubText
import com.gymtracker.ui.theme.Surface
import kotlinx.coroutines.flow.collectLatest

sealed class NavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home    : NavScreen("home",    "Log",     Icons.Default.FitnessCenter)
    object History : NavScreen("history", "History", Icons.Default.History)
    object Manage  : NavScreen("manage",  "Manage",  Icons.Default.Tune)
    object Settings: NavScreen("settings","Settings",Icons.Default.Settings)
}

val BOTTOM_NAV_ITEMS = listOf(
    NavScreen.Home,
    NavScreen.History,
    NavScreen.Manage,
    NavScreen.Settings
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackerTheme {
                GymTrackerApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTrackerApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.Success -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.Error   -> snackbarHostState.showSnackbar("⚠️ ${event.message}", duration = SnackbarDuration.Long)
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message.startsWith("⚠️")) Color(0xFF3A1A1A) else Color(0xFF1A2A00),
                    contentColor = if (data.visuals.message.startsWith("⚠️")) Color(0xFFFF6B6B) else Neon,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(containerColor = Color(0xFF0A0A0A), tonalElevation = 0.dp) {
                BOTTOM_NAV_ITEMS.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(NavScreen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint = if (selected) Neon else SubText
                            )
                        },
                        label = {
                            Text(
                                screen.label,
                                color = if (selected) Neon else SubText,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1A2A00)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(navController = navController, startDestination = NavScreen.Home.route) {
                composable(NavScreen.Home.route)     { HomeScreen(viewModel) }
                composable(NavScreen.History.route)  { HistoryScreen(viewModel) }
                composable(NavScreen.Manage.route)   { ManageScreen(viewModel) }
                composable(NavScreen.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }
}
