package com.gymtracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.history.HistoryScreen
import com.gymtracker.ui.home.HomeScreen
import com.gymtracker.ui.manage.ManageScreen
import com.gymtracker.ui.theme.*

enum class DashboardTab { LOG, HISTORY, MANAGE }

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(DashboardTab.LOG) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = SurfaceVariant,
            contentColor = Neon,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tab.ordinal]),
                    color = Neon,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = tab == DashboardTab.LOG,
                onClick = { tab = DashboardTab.LOG },
                icon = {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (tab == DashboardTab.LOG) Neon else SubText
                    )
                },
                text = { Text("Log", color = if (tab == DashboardTab.LOG) Neon else SubText) }
            )
            Tab(
                selected = tab == DashboardTab.HISTORY,
                onClick = { tab = DashboardTab.HISTORY },
                icon = {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = if (tab == DashboardTab.HISTORY) Neon else SubText
                    )
                },
                text = { Text("History", color = if (tab == DashboardTab.HISTORY) Neon else SubText) }
            )
            Tab(
                selected = tab == DashboardTab.MANAGE,
                onClick = { tab = DashboardTab.MANAGE },
                icon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (tab == DashboardTab.MANAGE) Neon else SubText
                    )
                },
                text = { Text("Manage", color = if (tab == DashboardTab.MANAGE) Neon else SubText) }
            )
        }

        when (tab) {
            DashboardTab.LOG     -> HomeScreen(viewModel)
            DashboardTab.HISTORY -> HistoryScreen(viewModel)
            DashboardTab.MANAGE  -> ManageScreen(viewModel)
        }
    }
}
