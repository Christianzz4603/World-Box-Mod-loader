package com.example.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressFraction: Float,
        val speedBytesPerSec: Long
    ) : DownloadState()

    data class Success(val downloadedFile: File, val md5Checksum: String) : DownloadState()
    data class Error(val message: String, val canRetry: Boolean = true) : DownloadState()
}

class DownloadManager(private val context: Context) {

    private val downloadsDir = File(context.filesDir, "downloads").apply {
        if (!exists()) mkdirs()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun downloadFile(
        url: String,
        targetFileName: String,
        expectedMd5: String? = null,
        maxRetries: Int = 2
    ): Flow<DownloadState> = flow {
        val destinationFile = File(downloadsDir, targetFileName)
        val tempFile = File(downloadsDir, "$targetFileName.tmp")

        var attempt = 0
        var success = false

        while (attempt <= maxRetries && !success) {
            attempt++
            try {
                if (tempFile.exists()) tempFile.delete()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WorldBoxModLauncher/1.0 (Android)")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP error ${response.code}: ${response.message}"
                    if (attempt > maxRetries) {
                        emit(DownloadState.Error(errMsg, canRetry = false))
                        return@flow
                    }
                    continue
                }

                val body = response.body
                if (body == null) {
                    if (attempt > maxRetries) {
                        emit(DownloadState.Error("Empty response body from server", canRetry = false))
                        return@flow
                    }
                    continue
                }

                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val digest = MessageDigest.getInstance("MD5")

                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    var bytesDownloaded = 0L
                    var lastTime = System.currentTimeMillis()
                    var bytesSinceLastTime = 0L
                    var speed = 0L

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        bytesDownloaded += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            speed = (bytesSinceLastTime * 1000) / elapsed
                            lastTime = now
                            bytesSinceLastTime = 0L
                        }

                        val progressFraction = if (totalBytes > 0) {
                            (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        emit(
                            DownloadState.Progress(
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                progressFraction = progressFraction,
                                speedBytesPerSec = speed
                            )
                        )
                    }
                }

                // Verify file size and checksum
                if (totalBytes > 0 && tempFile.length() != totalBytes) {
                    tempFile.delete()
                    if (attempt > maxRetries) {
                        emit(DownloadState.Error("Corrupted download: size mismatch.", canRetry = false))
                        return@flow
                    }
                    continue
                }

                val md5Hex = digest.digest().joinToString("") { "%02x".format(it) }
                if (!expectedMd5.isNullOrEmpty() && !md5Hex.equals(expectedMd5, ignoreCase = true)) {
                    tempFile.delete()
                    if (attempt > maxRetries) {
                        emit(DownloadState.Error("Checksum validation failed. Downloaded file corrupted.", canRetry = false))
                        return@flow
                    }
                    continue
                }

                if (destinationFile.exists()) destinationFile.delete()
                val renamed = tempFile.renameTo(destinationFile)
                if (!renamed) {
                    tempFile.copyTo(destinationFile, overwrite = true)
                    tempFile.delete()
                }

                success = true
                emit(DownloadState.Success(destinationFile, md5Hex))

            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                if (attempt > maxRetries) {
                    emit(DownloadState.Error("Download failed: ${e.localizedMessage}", canRetry = false))
                    return@flow
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun clearDownloads() {
        downloadsDir.listFiles()?.forEach { it.delete() }
    }
}
