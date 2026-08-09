package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entities.LauncherLogEntity
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.LaunchState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val wbInfo by viewModel.worldBoxInfo.collectAsState()
    val managedInfo by viewModel.managedApkInfo.collectAsState()
    val enabledMods by viewModel.enabledMods.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val launchState by viewModel.launchState.collectAsState()

    var showProfileDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_worldbox_banner),
                        contentDescription = "WorldBox Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "WorldBox Mod Launcher",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (wbInfo.isInstalled) "WorldBox Version: ${wbInfo.versionName}" else "WorldBox Mod Engine",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // WorldBox Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "WorldBox Status",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AssistChip(
                            onClick = { viewModel.refreshDetector() },
                            shape = RoundedCornerShape(4.dp),
                            label = {
                                Text(
                                    if (wbInfo.isInstalled) "Installed" else "Not Found",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (wbInfo.isInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (wbInfo.isInstalled) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    if (wbInfo.isInstalled) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Package: ${wbInfo.packageName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Version: ${wbInfo.versionName} (Build ${wbInfo.versionCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = wbInfo.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (managedInfo.exists) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column {
                                    Text(
                                        text = "Managed WorldBox APK Ready",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Version ${managedInfo.versionName ?: "1.0.0"} (${managedInfo.fileSize / (1024 * 1024)} MB)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Profile & Launch Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Mod Profile",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeProfile?.name ?: "Default Profile",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = { showProfileDropdown = true },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("profile_selector_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Profile", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }

                            DropdownMenu(
                                expanded = showProfileDropdown,
                                onDismissRequest = { showProfileDropdown = false }
                            ) {
                                profiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            viewModel.switchProfile(profile.id)
                                            showProfileDropdown = false
                                        },
                                        leadingIcon = {
                                            if (profile.id == activeProfile?.id) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { onNavigateToTab(1) },
                            shape = RoundedCornerShape(4.dp),
                            label = { Text("${enabledMods.size} Active Mods", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // PLAY BUTTON
                    Button(
                        onClick = { viewModel.launchGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("launch_worldbox_button"),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = launchState !is LaunchState.SyncingMods
                    ) {
                        if (launchState is LaunchState.SyncingMods) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Syncing Active Profile Mods...",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LAUNCH WORLDBOX",
                                style = MaterialTheme.typography.displayMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Quick Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = { onNavigateToTab(1) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Mods", style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                        Text("Installed Mods", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedCard(
                    onClick = { onNavigateToTab(2) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("GameBanana", style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                        Text("Browse Mods", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedCard(
                    onClick = { onNavigateToTab(4) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Settings", style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                        Text("APK & Storage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Recent System Activity
        item {
            Text(
                text = "System & Launch Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "No log records available yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs.take(5)) { log ->
                LogItemRow(log)
            }
        }
    }

    // Launch feedback dialog
    when (val state = launchState) {
        is LaunchState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetLaunchState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen) },
                title = { Text("WorldBox Launching", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetLaunchState() }) {
                        Text("OK", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
        is LaunchState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetLaunchState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Launch Issue", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    if (state.canImportApk) {
                        Button(
                            onClick = {
                                viewModel.resetLaunchState()
                                onNavigateToTab(4)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Import WorldBox APK", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        TextButton(onClick = { viewModel.resetLaunchState() }) {
                            Text("Close", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                dismissButton = {
                    if (state.canImportApk) {
                        TextButton(onClick = { viewModel.resetLaunchState() }) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            )
        }
        else -> {}
    }
}

@Composable
fun LogItemRow(log: LauncherLogEntity) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (icon, color) = when (log.level) {
                "SUCCESS" -> Icons.Default.CheckCircle to SuccessGreen
                "ERROR" -> Icons.Default.Error to MaterialTheme.colorScheme.error
                "WARN" -> Icons.Default.Warning to WarningAmber
                else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[${log.tag}] ${log.message}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
