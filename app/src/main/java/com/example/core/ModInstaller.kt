package com.example.core

import android.content.Context
import com.example.data.LauncherRepository
import com.example.data.entities.ModEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

    suspend fun installModFromArchive(
        archiveFile: File,
        gameBananaId: Long? = null,
        gameVersion: String? = null,
        iconUrl: String? = null,
        onProgress: (Float, String) -> Unit
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            val modUUID = UUID.randomUUID().toString()
            val targetModDir = File(modsStorageDir, modUUID)
            targetModDir.mkdirs()

            onProgress(0.1f, "Extracting mod archive...")

            val extractResult = if (archiveFile.extension.equals("zip", ignoreCase = true) ||
                archiveFile.extension.equals("ncmod", ignoreCase = true)
            ) {
                extractor.extractZip(archiveFile, targetModDir) { progress, msg ->
                    onProgress(0.1f + progress * 0.5f, msg)
                }
            } else {
                // If direct file (e.g., single .json or .dll), copy directly
                val copied = archiveFile.copyTo(File(targetModDir, archiveFile.name), overwrite = true)
                Result.success(listOf(copied))
            }

            if (extractResult.isFailure) {
                targetModDir.deleteRecursively()
                val err = extractResult.exceptionOrNull()?.message ?: "Extraction failed."
                return@withContext InstallResult.Failure(err)
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
                gameBananaId = gameBananaId,
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
            InstallResult.Success(modEntity, "Mod '${report.modName}' installed successfully! ${report.summary}")

        } catch (e: Exception) {
            InstallResult.Failure("Installation failed: ${e.localizedMessage}")
        }
    }
}
