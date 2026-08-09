package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.network.GbModFile
import com.example.network.GbModItem
import com.example.ui.viewmodel.GbDownloadProgressState
import com.example.ui.viewmodel.GbUiState
import com.example.ui.viewmodel.GameBananaViewModel
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBananaScreen(
    gbViewModel: GameBananaViewModel,
    mainViewModel: MainViewModel
) {
    val uiState by gbViewModel.uiState.collectAsState()
    val searchQuery by gbViewModel.searchQuery.collectAsState()
    val selectedSort by gbViewModel.selectedSort.collectAsState()
    val selectedModDetails by gbViewModel.selectedModDetails.collectAsState()
    val isLoadingDetails by gbViewModel.isLoadingDetails.collectAsState()
    val downloadProgressState by gbViewModel.downloadState.collectAsState()

    var activeSearchText by remember { mutableStateOf(searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = activeSearchText,
            onValueChange = {
                activeSearchText = it
                gbViewModel.setSearchQuery(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gb_search_input"),
            placeholder = { Text("Search WorldBox mods on GameBanana...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (activeSearchText.isNotEmpty()) {
                    IconButton(onClick = {
                        activeSearchText = ""
                        gbViewModel.setSearchQuery("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(4.dp)
        )

        // Sort Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSort == "new",
                onClick = { gbViewModel.setSort("new") },
                shape = RoundedCornerShape(4.dp),
                label = { Text("Latest Mods", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedSort == "popular",
                onClick = { gbViewModel.setSort("popular") },
                shape = RoundedCornerShape(4.dp),
                label = { Text("Popular", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedSort == "rating",
                onClick = { gbViewModel.setSort("rating") },
                shape = RoundedCornerShape(4.dp),
                label = { Text("Top Rated", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Main Content Area
        when (val state = uiState) {
            is GbUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Fetching live mods from GameBanana...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is GbUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Network Connection Issue", style = MaterialTheme.typography.titleLarge)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { gbViewModel.loadMods() }, shape = RoundedCornerShape(4.dp)) {
                            Text("Retry Connection", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            is GbUiState.Success -> {
                if (state.mods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No mods found on GameBanana.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.mods,
                            key = { it.id }
                        ) { modItem ->
                            GbModCard(
                                modItem = modItem,
                                onClick = { gbViewModel.selectModForDetails(modItem) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Mod details Modal Bottom Sheet
    selectedModDetails?.let { (modItem, files) ->
        ModalBottomSheet(
            onDismissRequest = { gbViewModel.clearSelectedModDetails() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = modItem.name ?: "Mod Details",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "By ${modItem.submitter?.name ?: "Unknown"} • Downloads: ${modItem.downloadCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!modItem.teaser.isNullOrBlank()) {
                    Text(
                        text = modItem.teaser,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "Available Download Files (${files.size})",
                    style = MaterialTheme.typography.titleLarge
                )

                files.forEach { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (file.fileSize > 0) "${file.fileSize / 1024} KB" else "Standard Archive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    gbViewModel.clearSelectedModDetails()
                                    gbViewModel.downloadAndInstallMod(mainViewModel, modItem, file)
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                                modifier = Modifier.testTag("download_file_${file.id}")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Install", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Download & Install Progress Dialog
    when (val state = downloadProgressState) {
        is GbDownloadProgressState.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Installing ${state.modName}", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.fileName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { state.progressFraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.speedBytesPerSec > 0) {
                            Text(
                                text = "Speed: ${state.speedBytesPerSec / 1024} KB/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        is GbDownloadProgressState.Completed -> {
            AlertDialog(
                onDismissRequest = { gbViewModel.dismissDownloadProgress() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Installation Complete", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(onClick = { gbViewModel.dismissDownloadProgress() }, shape = RoundedCornerShape(4.dp)) {
                        Text("Done", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }

        is GbDownloadProgressState.Error -> {
            AlertDialog(
                onDismissRequest = { gbViewModel.dismissDownloadProgress() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Installation Issue", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { gbViewModel.dismissDownloadProgress() }) {
                        Text("Close", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }

        else -> {}
    }
}

@Composable
fun GbModCard(
    modItem: GbModItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageUrl = modItem.previewMedia?.images?.firstOrNull()?.getFullUrl()
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = modItem.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = modItem.name ?: "Unnamed Mod",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = modItem.submitter?.name ?: "GameBanana Author",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "${modItem.downloadCount}", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "${modItem.likeCount}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
