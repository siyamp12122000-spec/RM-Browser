package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isDangerous: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class BrowserDownloadManager(private val context: Context) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val dangerousExtensions = setOf(
        ".apk", ".exe", ".bat", ".cmd", ".vbs", ".scr", ".msi", ".jar", ".sh", ".ps1"
    )

    fun isDangerousFile(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        return dangerousExtensions.any { lower.endsWith(it) }
    }

    fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ): DownloadItem? {
        return try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val isDangerous = isDangerousFile(fileName)

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading file via RM Browser")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            val downloadId = manager?.enqueue(request) ?: System.currentTimeMillis()

            val item = DownloadItem(
                id = downloadId,
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                sizeBytes = contentLength,
                isDangerous = isDangerous
            )

            _downloads.value = listOf(item) + _downloads.value
            item
        } catch (_: Exception) {
            null
        }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
    }
}
