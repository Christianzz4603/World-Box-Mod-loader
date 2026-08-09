package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherApp(
    mainViewModel: MainViewModel = viewModel()
) {
    val selectedTab by mainViewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "WorldBox Launcher"
                            1 -> "Mod Manager"
                            2 -> "Mod Profiles"
                            3 -> "Launcher Settings"
                            else -> "WorldBox Launcher"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { mainViewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = {
                        Text(
                            text = "Home",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { mainViewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Mods") },
                    label = {
                        Text(
                            text = "Mods",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_mods")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { mainViewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Profiles") },
                    label = {
                        Text(
                            text = "Profiles",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_profiles")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { mainViewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = {
                        Text(
                            text = "Settings",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = mainViewModel, onNavigateToTab = { mainViewModel.setSelectedTab(it) })
                1 -> ModsScreen(viewModel = mainViewModel)
                2 -> ProfilesScreen(viewModel = mainViewModel)
                3 -> SettingsScreen(viewModel = mainViewModel)
            }
        }
    }
}
