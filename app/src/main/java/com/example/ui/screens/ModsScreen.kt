package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.ModEntity
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ModImportState

@Composable
fun ModsScreen(viewModel: MainViewModel) {
    val mods by viewModel.mods.collectAsState()
    val modImportState by viewModel.modImportState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedModForDetails by remember { mutableStateOf<ModEntity?>(null) }

    val modPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importModFromUri(uri)
        }
    }

    val filteredMods = remember(mods, searchQuery) {
        if (searchQuery.isBlank()) mods
        else mods.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.format.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mods_search_input"),
            placeholder = { Text("Search installed mods...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Installed Mods (${mods.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${mods.count { it.isEnabled }} Enabled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { modPickerLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("import_mod_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Mod", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (filteredMods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (searchQuery.isEmpty()) "No mods installed yet." else "No matching mods found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Import mod archives (.zip, .ncmod, .json, etc.) via file selector.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { modPickerLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("empty_state_import_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Mod File", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = filteredMods,
                    key = { _, mod -> mod.id }
                ) { index, mod ->
                    ModCardItem(
                        mod = mod,
                        onToggleEnabled = { viewModel.toggleModEnabled(mod) },
                        onMoveUp = if (index > 0) {
                            { viewModel.updateLoadOrder(mod.id, index - 1) }
                        } else null,
                        onMoveDown = if (index < filteredMods.size - 1) {
                            { viewModel.updateLoadOrder(mod.id, index + 1) }
                        } else null,
                        onClickDetails = { selectedModForDetails = mod },
                        onDelete = { viewModel.deleteMod(mod) }
                    )
                }
            }
        }
    }

    // Mod Import Dialogs
    when (val state = modImportState) {
        is ModImportState.Progress -> {
            AlertDialog(
                onDismissRequest = { /* Non-dismissable during active extraction */ },
                shape = RoundedCornerShape(4.dp),
                icon = { CircularProgressIndicator(modifier = Modifier.size(32.dp)) },
                title = { Text("Importing Mod...", style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.status, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { state.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {}
            )
        }
        is ModImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetModImportState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen) },
                title = { Text("Mod Imported!", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetModImportState() }) {
                        Text("OK", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
        is ModImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetModImportState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Import Failed", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetModImportState() }) {
                        Text("Close", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }
        ModImportState.Idle -> {}
    }

    // Mod Detail Dialog
    selectedModForDetails?.let { mod ->
        AlertDialog(
            onDismissRequest = { selectedModForDetails = null },
            shape = RoundedCornerShape(4.dp),
            icon = { Icon(Icons.Default.Extension, contentDescription = null) },
            title = { Text(mod.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Author: ${mod.author}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Version: ${mod.version}", style = MaterialTheme.typography.bodyMedium)
                    Text("Format: ${mod.format}", style = MaterialTheme.typography.bodyMedium)
                    Text("Target Game Version: ${mod.targetGameVersion}", style = MaterialTheme.typography.bodyMedium)
                    Text("Local Path: ${mod.localPath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    Text("Description:", style = MaterialTheme.typography.labelLarge)
                    Text(mod.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text("Compatibility Status:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = mod.compatibilityNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mod.isAndroidCompatible) SuccessGreen else WarningAmber
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedModForDetails = null }) {
                    Text("Close", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMod(mod)
                        selectedModForDetails = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Uninstall Mod", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
fun ModCardItem(
    mod: ModEntity,
    onToggleEnabled: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onClickDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.5.dp, if (mod.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (mod.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Switch(
                        checked = mod.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.testTag("mod_switch_${mod.id}")
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mod.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                        Text(
                            text = "by ${mod.author} • v${mod.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                // Format badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = mod.format,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Compatibility note row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (mod.isAndroidCompatible) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (mod.isAndroidCompatible) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = mod.compatibilityNotes,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mod.isAndroidCompatible) SuccessGreen else WarningAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Actions
                Row {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onClickDetails, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Details", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
