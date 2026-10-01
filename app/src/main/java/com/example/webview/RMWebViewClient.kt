package com.example.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.BrowserSettings
import com.example.security.KahafGuardEngine
import com.example.security.SecurityEvaluationResult
import com.example.security.ThreatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class RMWebViewClient(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val kahafGuardEngine: KahafGuardEngine,
    private val getSettings: () -> BrowserSettings,
    private val onPageStartedCallback: (url: String) -> Unit,
    private val onPageFinishedCallback: (url: String) -> Unit,
    private val onUrlBlockedCallback: (result: SecurityEvaluationResult) -> Unit,
    private val onSslErrorCallback: (url: String, errorMsg: String) -> Unit,
    private val onErrorCallback: (errorCode: Int, description: String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        val uri = request.url

        // Handle external protocols safely
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https" && scheme != "about") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (_: Exception) {
                // Ignore unsupported app schemes
            }
            return true
        }

        // Evaluate with Kahaf Guard engine before allowing navigation
        val currentSettings = getSettings()
        coroutineScope.launch {
            val eval = kahafGuardEngine.evaluateUrl(url, currentSettings)
            if (eval.isBlocked) {
                withContext(Dispatchers.Main) {
                    view?.stopLoading()
                    onUrlBlockedCallback(eval)
                }
            } else {
                withContext(Dispatchers.Main) {
                    view?.loadUrl(url)
                }
            }
        }
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        val host = request.url?.host ?: return null
        val settings = getSettings()

        // Block embedded ad and tracker sub-resources if enabled
        if (settings.blockAds || settings.blockTrackers) {
            val threat = kahafGuardEngine.isAdOrTracker(host)
            if (threat == ThreatType.AD && settings.blockAds) {
                // Return empty response to block the ad network request
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }
            if (threat == ThreatType.TRACKER && settings.blockTrackers) {
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStartedCallback(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinishedCallback(it) }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        // CRITICAL SECURITY MANDATE: Never bypass SSL certificate errors
        handler?.cancel()
        val errorDesc = when (error?.primaryError) {
            SslError.SSL_EXPIRED -> "The SSL certificate for this site has expired."
            SslError.SSL_IDMISMATCH -> "Hostname mismatch. This certificate does not match the website."
            SslError.SSL_UNTRUSTED -> "The certificate authority is untrusted or self-signed."
            SslError.SSL_NOTYETVALID -> "The certificate is not yet valid."
            else -> "Untrusted or invalid SSL connection detected."
        }
        val failingUrl = error?.url ?: view?.url ?: "Unknown URL"
        onSslErrorCallback(failingUrl, errorDesc)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (request?.isForMainFrame == true) {
            val code = error?.errorCode ?: 0
            val desc = error?.description?.toString() ?: "Connection error"
            onErrorCallback(code, desc)
        }
    }
}
