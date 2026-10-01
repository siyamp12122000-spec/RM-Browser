package com.example.webview

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class RMWebChromeClient(
    private val onProgressUpdate: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit,
    private val onFaviconReceived: (Bitmap?) -> Unit,
    private val onShowCustomViewCallback: (View, WebChromeClient.CustomViewCallback) -> Unit,
    private val onHideCustomViewCallback: () -> Unit,
    private val onShowFileChooserCallback: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        onFaviconReceived(icon)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            onShowCustomViewCallback(view, callback)
        }
    }

    override fun onHideCustomView() {
        onHideCustomViewCallback()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooserCallback(filePathCallback, fileChooserParams)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // Safe default: deny automatic geolocation tracking unless explicitly allowed
        callback?.invoke(origin, false, false)
    }
}
