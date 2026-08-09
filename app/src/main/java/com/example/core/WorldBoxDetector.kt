package com.example.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

data class WorldBoxInfo(
    val isInstalled: Boolean,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val appLabel: String? = null,
    val icon: Drawable? = null,
    val sourceApkPath: String? = null,
    val isApkPathAccessible: Boolean = false,
    val statusMessage: String
)

class WorldBoxDetector(private val context: Context) {

    companion object {
        val KNOWN_PACKAGE_NAMES = listOf(
            "com.mkarpenko.worldbox",
            "com.mkarpenko.worldbox.premium"
        )
    }

    fun detectWorldBox(): WorldBoxInfo {
        val pm = context.packageManager

        for (pkg in KNOWN_PACKAGE_NAMES) {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }

                val appInfo = packageInfo.applicationInfo ?: continue
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val versionName = packageInfo.versionName ?: "Unknown"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val sourceDir = appInfo?.sourceDir
                var isAccessible = false
                if (!sourceDir.isNullOrEmpty()) {
                    val file = java.io.File(sourceDir)
                    isAccessible = file.exists() && file.canRead()
                }

                val statusMsg = if (isAccessible) {
                    "WorldBox v$versionName ($pkg) detected and accessible."
                } else {
                    "WorldBox v$versionName detected. Android OS restricts APK reading; import APK manually for isolated launcher copies."
                }

                return WorldBoxInfo(
                    isInstalled = true,
                    packageName = pkg,
                    versionName = versionName,
                    versionCode = versionCode,
                    appLabel = label,
                    icon = icon,
                    sourceApkPath = sourceDir,
                    isApkPathAccessible = isAccessible,
                    statusMessage = statusMsg
                )

            } catch (e: PackageManager.NameNotFoundException) {
                // Not found for this package name, check next
            } catch (e: Exception) {
                // Ignore and keep checking
            }
        }

        return WorldBoxInfo(
            isInstalled = false,
            statusMessage = "WorldBox is not installed on this device. You can import a WorldBox APK in Settings."
        )
    }
}
