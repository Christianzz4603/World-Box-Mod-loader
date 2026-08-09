package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.data.entities.ModEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class LaunchResult {
    data class Success(val message: String) : LaunchResult()
    data class Failure(val reason: String, val canImportApk: Boolean = false) : LaunchResult()
}

class WorldBoxLauncher(
    private val context: Context,
    private val detector: WorldBoxDetector,
    private val apkManager: ApkManager,
    private val modLoader: ModLoader
) {

    suspend fun launchGame(
        enabledMods: List<ModEntity>,
        activeProfileName: String
    ): LaunchResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: Sync active profile mods to loader directory
            val syncResult = modLoader.syncActiveMods(enabledMods)
            if (syncResult.isFailure) {
                return@withContext LaunchResult.Failure(
                    "Failed to sync mods before launch: ${syncResult.exceptionOrNull()?.message}"
                )
            }

            // Step 2: Detect original installed app
            val wbInfo = detector.detectWorldBox()
            if (wbInfo.isInstalled && wbInfo.packageName != null) {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(wbInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext LaunchResult.Success(
                        "Launched installed WorldBox (${wbInfo.packageName}) with '$activeProfileName' (${enabledMods.size} active mods)."
                    )
                }
            }

            // Step 3: Check managed APK copy if original app isn't installed
            val managedInfo = apkManager.getManagedApkInfo()
            if (managedInfo.exists && managedInfo.filePath != null) {
                val apkFile = File(managedInfo.filePath)
                if (apkFile.exists()) {
                    // Prompt Android PackageInstaller or launcher intent
                    val apkUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(installIntent)
                    return@withContext LaunchResult.Success(
                        "Opening package installer for managed WorldBox copy. Active profile '$activeProfileName' configured."
                    )
                }
            }

            // Fallback: Neither installed nor managed copy found
            LaunchResult.Failure(
                reason = "WorldBox is not installed on this device and no managed APK copy was found.",
                canImportApk = true
            )

        } catch (e: Exception) {
            LaunchResult.Failure("Launch error: ${e.localizedMessage}")
        }
    }
}
