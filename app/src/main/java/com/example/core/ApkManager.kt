package com.example.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class ManagedApkInfo(
    val exists: Boolean,
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val statusMessage: String
)

class ApkManager(private val context: Context) {

    private val managedDir = File(context.filesDir, "worldbox_managed").apply {
        if (!exists()) mkdirs()
    }

    private val managedApkFile = File(managedDir, "base.apk")

    fun getManagedApkInfo(): ManagedApkInfo {
        if (!managedApkFile.exists() || managedApkFile.length() == 0L) {
            return ManagedApkInfo(
                exists = false,
                statusMessage = "No launcher-managed WorldBox APK stored."
            )
        }

        val pm = context.packageManager
        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(managedApkFile.absolutePath, 0)

        if (archiveInfo == null) {
            return ManagedApkInfo(
                exists = false,
                filePath = managedApkFile.absolutePath,
                fileSize = managedApkFile.length(),
                statusMessage = "Stored file is invalid or corrupted APK."
            )
        }

        return ManagedApkInfo(
            exists = true,
            filePath = managedApkFile.absolutePath,
            fileSize = managedApkFile.length(),
            packageName = archiveInfo.packageName,
            versionName = archiveInfo.versionName ?: "Unknown",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            },
            statusMessage = "Managed WorldBox APK ready (v${archiveInfo.versionName})."
        )
    }

    suspend fun importApkFromUri(
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): Result<ManagedApkInfo> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Opening APK file stream...")
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open Uri stream."))

            val tempFile = File(managedDir, "temp_import.apk")
            if (tempFile.exists()) tempFile.delete()

            onProgress(0.1f, "Copying APK to launcher private sandbox...")
            val totalBytes = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L
            var copiedBytes = 0L

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        copiedBytes += read
                        if (totalBytes > 0) {
                            val progress = 0.1f + (copiedBytes.toFloat() / totalBytes.toFloat()) * 0.7f
                            onProgress(progress, "Copying APK: ${copiedBytes / (1024 * 1024)}MB / ${totalBytes / (1024 * 1024)}MB")
                        }
                    }
                }
            }

            onProgress(0.85f, "Validating APK structure and package info...")
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, 0)

            if (packageInfo == null) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Selected file is not a valid Android APK package."))
            }

            if (!WorldBoxDetector.KNOWN_PACKAGE_NAMES.contains(packageInfo.packageName)) {
                onProgress(0.9f, "Warning: Package name '${packageInfo.packageName}' differs from standard WorldBox, but importing...")
            }

            if (managedApkFile.exists()) managedApkFile.delete()
            val renamed = tempFile.renameTo(managedApkFile)
            if (!renamed) {
                tempFile.copyTo(managedApkFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1.0f, "Import successful!")
            Result.success(getManagedApkInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearManagedApk(): Boolean = withContext(Dispatchers.IO) {
        if (managedApkFile.exists()) {
            managedApkFile.delete()
        } else true
    }
}
