package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BrowserSettings
import com.example.download.BrowserDownloadManager
import com.example.security.KahafGuardEngine
import com.example.security.SecurityEvaluationResult

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RMWebViewContainer(
    tabId: String,
    urlToLoad: String,
    settings: BrowserSettings,
    kahafGuardEngine: KahafGuardEngine,
    downloadManager: BrowserDownloadManager,
    isIncognito: Boolean,
    onPageStarted: (url: String) -> Unit,
    onPageFinished: (url: String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleReceived: (String) -> Unit,
    onUrlBlocked: (SecurityEvaluationResult) -> Unit,
    onSslError: (url: String, error: String) -> Unit,
    onError: (code: Int, desc: String) -> Unit,
    onRegisterWebView: (WebView) -> Unit,
    onShowCustomView: (View, android.webkit.WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    onShowFileChooser: (android.webkit.ValueCallback<Array<android.net.Uri>>?, android.webkit.WebChromeClient.FileChooserParams?) -> Boolean,
    onDangerousDownloadPrompt: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val webView = remember(tabId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Secure web settings configuration
            this.settings.apply {
                javaScriptEnabled = settings.javascriptEnabled
                domStorageEnabled = true
                databaseEnabled = true
                // Disallow direct access to app private filesystem files via URLs
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true

                if (settings.desktopMode) {
                    userAgentString = DESKTOP_USER_AGENT
                }

                // Privacy: Do Not Track
                if (settings.doNotTrack) {
                    // Modern standard header handled via request headers if supported
                }
            }

            // Cookie handling
            val cookieManager = CookieManager.getInstance()
            if (isIncognito) {
                cookieManager.setAcceptCookie(false)
            } else {
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, !settings.blockTrackers)
            }

            // WebChromeClient
            webChromeClient = RMWebChromeClient(
                onProgressUpdate = onProgressChanged,
                onTitleReceived = onTitleReceived,
                onFaviconReceived = { /* optional icon caching */ },
                onShowCustomViewCallback = onShowCustomView,
                onHideCustomViewCallback = onHideCustomView,
                onShowFileChooserCallback = onShowFileChooser
            )

            // WebViewClient
            webViewClient = RMWebViewClient(
                context = context,
                coroutineScope = coroutineScope,
                kahafGuardEngine = kahafGuardEngine,
                getSettings = { settings },
                onPageStartedCallback = onPageStarted,
                onPageFinishedCallback = onPageFinished,
                onUrlBlockedCallback = onUrlBlocked,
                onSslErrorCallback = onSslError,
                onErrorCallback = onError
            )

            // Download Listener
            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                val fileName = android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                if (downloadManager.isDangerousFile(fileName)) {
                    onDangerousDownloadPrompt(fileName) {
                        downloadManager.startDownload(
                            downloadUrl,
                            userAgent,
                            contentDisposition,
                            mimetype,
                            contentLength
                        )
                    }
                } else {
                    downloadManager.startDownload(
                        downloadUrl,
                        userAgent,
                        contentDisposition,
                        mimetype,
                        contentLength
                    )
                }
            }
        }
    }

    // Register active web view instance with parent controller
    DisposableEffect(webView) {
        onRegisterWebView(webView)
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        update = { wv ->
            // Update desktop mode user agent dynamically
            if (settings.desktopMode) {
                wv.settings.userAgentString = DESKTOP_USER_AGENT
            } else {
                wv.settings.userAgentString = null
            }
            wv.settings.javaScriptEnabled = settings.javascriptEnabled

            // Load URL if changed
            if (wv.url != urlToLoad && urlToLoad != "about:blank" && urlToLoad.isNotBlank()) {
                wv.loadUrl(urlToLoad)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
