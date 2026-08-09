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

    fun isZipArchive(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                if (fis.read(header) == 4) {
                    header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (Float, String) -> Unit
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return@withContext Result.failure(Exception("Archive file is empty or does not exist."))
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val extractedFiles = mutableListOf<File>()
            val canonicalDestDir = targetDir.canonicalFile

            // Count total entries first for progress reporting
            var totalEntries = 0
            try {
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    while (zis.nextEntry != null) {
                        totalEntries++
                        zis.closeEntry()
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Corrupted or invalid zip archive structure."))
            }

            if (totalEntries == 0) {
                return@withContext Result.failure(Exception("Zip archive contains no entries."))
            }

            var processedEntries = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val newFile = File(targetDir, entryName)

                    // Path Traversal Security Check (Zip Slip Vulnerability Protection)
                    if (!newFile.canonicalFile.path.startsWith(canonicalDestDir.path)) {
                        throw SecurityException("Zip Slip path traversal attempt blocked: $entryName")
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

            if (extractedFiles.isEmpty()) {
                Result.failure(Exception("No files were extracted from archive."))
            } else {
                Result.success(extractedFiles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

