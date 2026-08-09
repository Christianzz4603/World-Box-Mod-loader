package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ApkImportState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val managedApkInfo by viewModel.managedApkInfo.collectAsState()
    val apkImportState by viewModel.apkImportState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importApkFromUri(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: APK Import & Managed Copy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "WorldBox APK Management",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Text(
                        text = "Android OS restricts direct reading of installed app APKs on newer versions. You can import a WorldBox .apk file directly from your storage for isolated launcher copies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    if (managedApkInfo.exists) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Managed APK Present",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Package: ${managedApkInfo.packageName ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Version: ${managedApkInfo.versionName} (${managedApkInfo.fileSize / (1024 * 1024)} MB)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_apk_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import APK", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                        }

                        if (managedApkInfo.exists) {
                            OutlinedButton(
                                onClick = { viewModel.clearManagedApk() },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Copy", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // Section: Storage & Maintenance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "Storage & Sandbox Cleanup",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Launcher Cache & Downloads", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Clear downloaded temp archives and cached files", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.downloadManager.clearDownloads() },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Clear Cache", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Section: Detailed Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detailed System Logs (${logs.size})",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Logs", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Text("No log records.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(logs) { log ->
                LogItemRow(log)
            }
        }
    }

    // Import APK progress dialog
    when (val state = apkImportState) {
        is ApkImportState.Progress -> {
            AlertDialog(
                onDismissRequest = {},
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                title = { Text("Importing WorldBox APK", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { state.fraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(state.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {}
            )
        }

        is ApkImportState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetApkImportState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("APK Imported", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.info.statusMessage, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(onClick = { viewModel.resetApkImportState() }, shape = RoundedCornerShape(4.dp)) {
                        Text("Done", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }

        is ApkImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetApkImportState() },
                shape = RoundedCornerShape(4.dp),
                icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Import Failed", style = MaterialTheme.typography.titleLarge) },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetApkImportState() }) {
                        Text("Close", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }

        else -> {}
    }
}
