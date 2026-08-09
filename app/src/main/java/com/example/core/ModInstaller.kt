package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.LauncherRepository
import com.example.data.entities.ModEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

sealed class InstallResult {
    data class Success(val modEntity: ModEntity, val message: String) : InstallResult()
    data class Failure(val reason: String) : InstallResult()
}

class ModInstaller(
    private val context: Context,
    private val repository: LauncherRepository,
    private val extractor: ArchiveExtractor,
    private val compatibilityChecker: CompatibilityChecker
) {

    private val modsStorageDir = File(context.filesDir, "mods").apply {
        if (!exists()) mkdirs()
    }

    suspend fun installModFromUri(
        uri: Uri,
        gameVersion: String? = null,
        onProgress: (Float, String) -> Unit
    ): InstallResult = withContext(Dispatchers.IO) {
        // Attempt to persist URI permissions if supported by provider
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Ignore if provider doesn't support persistable permissions
        }

        val displayName = getFileNameFromUri(context, uri)
        onProgress(0.05f, "Opening stream for '$displayName'...")

        val inputStream: InputStream? = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }

        if (inputStream == null) {
            return@withContext InstallResult.Failure("Unable to open file stream from selected document provider.")
        }

        val tempDir = File(context.cacheDir, "mod_imports").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "temp_${System.currentTimeMillis()}_${displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}")

        try {
            var copiedBytes = 0L
            val totalBytes = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: Exception) { -1L }

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        copiedBytes += read
                        if (totalBytes > 0) {
                            val progress = 0.05f + (copiedBytes.toFloat() / totalBytes.toFloat()) * 0.25f
                            onProgress(progress, "Copying mod file (${copiedBytes / 1024} KB)...")
                        }
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext InstallResult.Failure("Corrupted or empty mod file selected.")
            }

            val result = installModFromArchive(
                archiveFile = tempFile,
                originalFileName = displayName,
                gameVersion = gameVersion,
                onProgress = { p, msg ->
                    onProgress(0.3f + p * 0.7f, msg)
                }
            )

            tempFile.delete()
            return@withContext result
        } catch (e: Exception) {
            tempFile.delete()
            return@withContext InstallResult.Failure("Import failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    suspend fun installModFromArchive(
        archiveFile: File,
        originalFileName: String? = null,
        gameVersion: String? = null,
        iconUrl: String? = null,
        onProgress: (Float, String) -> Unit
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            val nameToCheck = originalFileName ?: archiveFile.name
            val ext = nameToCheck.substringAfterLast('.', "").lowercase()

            if (ext == "7z" || ext == "rar") {
                if (!extractor.isZipArchive(archiveFile)) {
                    return@withContext InstallResult.Failure("Unsupported format: .$ext archives are not supported directly. Please convert or extract to .zip or .ncmod format.")
                }
            }

            val modUUID = UUID.randomUUID().toString()
            val targetModDir = File(modsStorageDir, modUUID)
            targetModDir.mkdirs()

            onProgress(0.1f, "Extracting mod package...")

            val isZip = extractor.isZipArchive(archiveFile) || ext == "zip" || ext == "ncmod"
            val extractResult = if (isZip) {
                extractor.extractZip(archiveFile, targetModDir) { progress, msg ->
                    onProgress(0.1f + progress * 0.5f, msg)
                }
            } else {
                // If direct file (e.g. single .json, .dll, or .so), copy directly into target directory
                try {
                    val destName = if (originalFileName != null && originalFileName.isNotBlank()) originalFileName else archiveFile.name
                    val copied = archiveFile.copyTo(File(targetModDir, destName), overwrite = true)
                    Result.success(listOf(copied))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (extractResult.isFailure) {
                targetModDir.deleteRecursively()
                val err = extractResult.exceptionOrNull()?.message ?: "Invalid mod file or corrupted archive."
                return@withContext InstallResult.Failure("Invalid mod file: $err")
            }

            onProgress(0.7f, "Checking Android compatibility and parsing manifest...")
            val report = compatibilityChecker.inspectExtractedMod(targetModDir, gameVersion)

            onProgress(0.9f, "Saving mod to launcher database...")
            val modEntity = ModEntity(
                id = modUUID,
                modId = report.modName.lowercase().replace("\\s+".toRegex(), "_"),
                name = report.modName,
                author = report.modAuthor,
                version = report.modVersion,
                description = report.description,
                category = report.format,
                localPath = targetModDir.absolutePath,
                format = report.format,
                isAndroidCompatible = report.isCompatible,
                compatibilityNotes = report.summary,
                targetGameVersion = report.targetGameVersion,
                dateAdded = System.currentTimeMillis(),
                iconUrl = iconUrl,
                fileSize = archiveFile.length(),
                isEnabled = true,
                loadOrder = 0
            )

            repository.insertMod(modEntity)
            repository.log(
                level = if (report.isCompatible) "INFO" else "WARN",
                tag = "ModInstaller",
                message = "Installed '${report.modName}' v${report.modVersion}. ${report.summary}"
            )

            onProgress(1.0f, "Installation complete!")
            InstallResult.Success(modEntity, "Mod '${report.modName}' imported successfully! ${report.summary}")

        } catch (e: Exception) {
            InstallResult.Failure("Import failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            fileName = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // query failed, fallback to lastPathSegment
            }
        }
        if (fileName.isNullOrBlank()) {
            fileName = uri.lastPathSegment?.substringAfterLast('/')
        }
        return if (!fileName.isNullOrBlank()) fileName!! else "imported_mod.zip"
    }
}

