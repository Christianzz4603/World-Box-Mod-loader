package com.example.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            if (wbInfo.isInstalled && !wbInfo.packageName.isNullOrBlank()) {
                val pm = context.packageManager
                val targetPkg = wbInfo.packageName

                // Try retrieving launch intent via PackageManager
                var launchIntent: Intent? = try {
                    pm.getLaunchIntentForPackage(targetPkg)
                } catch (e: Exception) {
                    null
                }

                // Fallback to explicit MAIN/LAUNCHER intent if getLaunchIntentForPackage returned null
                if (launchIntent == null) {
                    try {
                        val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            setPackage(targetPkg)
                        }
                        val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.queryIntentActivities(explicitIntent, PackageManager.ResolveInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.queryIntentActivities(explicitIntent, 0)
                        }
                        if (resolveList.isNotEmpty()) {
                            launchIntent = explicitIntent
                        }
                    } catch (e: Exception) {
                        // ignore resolution errors
                    }
                }

                if (launchIntent != null) {
                    try {
                        // Ensure NEW_TASK flag is set cleanly for application context
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return@withContext LaunchResult.Success(
                            "Launched installed WorldBox ($targetPkg) with profile '$activeProfileName' (${enabledMods.size} active mods)."
                        )
                    } catch (e: Exception) {
                        // If startActivity fails (e.g. ActivityNotFoundException), fall through to managed copy or failure
                    }
                }
            }

            // Step 3: Check managed APK copy if original app isn't installed or couldn't start
            val managedInfo = apkManager.getManagedApkInfo()
            if (managedInfo.exists && managedInfo.filePath != null) {
                val apkFile = File(managedInfo.filePath)
                if (apkFile.exists() && apkFile.length() > 0) {
                    try {
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
                    } catch (e: Exception) {
                        return@withContext LaunchResult.Failure(
                            "Unable to launch package installer: ${e.localizedMessage ?: e.javaClass.simpleName}"
                        )
                    }
                }
            }

            // Step 4: Fallback when WorldBox is not installed and no managed APK is found
            LaunchResult.Failure(
                reason = "WorldBox is not installed on this device and no managed APK copy was found. Please install WorldBox or import a WorldBox APK in Settings.",
                canImportApk = true
            )

        } catch (e: Exception) {
            LaunchResult.Failure("Launch error: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }
}

