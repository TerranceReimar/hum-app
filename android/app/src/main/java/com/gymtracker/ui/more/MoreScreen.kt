package com.gymtracker.ui.more

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
import com.gymtracker.ui.profile.ProfileTabScreen
import com.gymtracker.ui.settings.SettingsScreen
import com.gymtracker.ui.theme.*

enum class MoreTab { PROFILE, SETTINGS }

@Composable
fun MoreScreen(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(MoreTab.PROFILE) }

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
                selected = tab == MoreTab.PROFILE,
                onClick = { tab = MoreTab.PROFILE },
                icon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (tab == MoreTab.PROFILE) Neon else SubText
                    )
                },
                text = { Text("Profile", color = if (tab == MoreTab.PROFILE) Neon else SubText) }
            )
            Tab(
                selected = tab == MoreTab.SETTINGS,
                onClick = { tab = MoreTab.SETTINGS },
                icon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (tab == MoreTab.SETTINGS) Neon else SubText
                    )
                },
                text = { Text("Settings", color = if (tab == MoreTab.SETTINGS) Neon else SubText) }
            )
        }

        when (tab) {
            MoreTab.PROFILE  -> ProfileTabScreen(viewModel)
            MoreTab.SETTINGS -> SettingsScreen(viewModel)
        }
    }
}
