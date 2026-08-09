package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ArchiveExtractor {

    suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (Float, String) -> Unit
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val extractedFiles = mutableListOf<File>()
            val canonicalDestDir = targetDir.canonicalFile

            // Count total entries first for progress reporting
            var totalEntries = 0
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                while (zis.nextEntry != null) {
                    totalEntries++
                }
            }

            if (totalEntries == 0) totalEntries = 1
            var processedEntries = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)

                    // Path Traversal Security Check (Zip Slip Vulnerability Protection)
                    if (!newFile.canonicalFile.path.startsWith(canonicalDestDir.path)) {
                        throw SecurityException("Zip Slip path traversal attempt blocked: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(16 * 1024)
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        extractedFiles.add(newFile)
                    }

                    processedEntries++
                    val progress = (processedEntries.toFloat() / totalEntries.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress, "Extracting ${entry.name}...")

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            Result.success(extractedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
