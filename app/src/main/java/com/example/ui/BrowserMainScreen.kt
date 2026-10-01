package com.example.ui

import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BrowserTab
import com.example.ui.components.BottomNavBar
import com.example.ui.components.SecurityInspectorDialog
import com.example.ui.components.TopSearchBar
import com.example.ui.screens.BlockedWarningScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrowserHomeScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IslamicSafeModeScreen
import com.example.ui.screens.SecurityDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.ThreatRed
import com.example.viewmodel.BrowserScreen
import com.example.viewmodel.BrowserViewModel
import com.example.webview.RMWebViewContainer

@Composable
fun BrowserMainScreen(
    viewModel: BrowserViewModel,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dnsState by viewModel.dnsState.collectAsStateWithLifecycle()

    val totalThreatsBlocked by viewModel.totalThreatsBlocked.collectAsStateWithLifecycle()
    val malwarePhishingBlocked by viewModel.malwarePhishingBlocked.collectAsStateWithLifecycle()
    val adsBlocked by viewModel.adsBlocked.collectAsStateWithLifecycle()
    val trackersBlocked by viewModel.trackersBlocked.collectAsStateWithLifecycle()
    val islamicSafeBlocked by viewModel.islamicSafeBlocked.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentSecurityEvents.collectAsStateWithLifecycle()
    val userRules by viewModel.userFilterRules.collectAsStateWithLifecycle()

    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val bookmarksList by viewModel.bookmarksList.collectAsStateWithLifecycle()
    val downloadsList by viewModel.downloadManager.downloads.collectAsStateWithLifecycle()

    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
    val showSecurityInspector by viewModel.showSecurityInspector.collectAsStateWithLifecycle()

    // Full screen HTML5 video custom view state
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Intercept back button for screens and web history
    BackHandler(enabled = true) {
        if (customVideoView != null) {
            customVideoCallback?.onCustomViewHidden()
            customVideoView = null
            customVideoCallback = null
            return@BackHandler
        }

        if (currentScreen != BrowserScreen.Browser) {
            viewModel.closeScreen()
            return@BackHandler
        }

        val handled = viewModel.goBack()
        if (!handled) {
            // Let system handle default back
            (context as? android.app.Activity)?.finish()
        }
    }

    // Fullscreen video overlay
    if (customVideoView != null) {
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    addView(customVideoView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    when (currentScreen) {
        is BrowserScreen.TabSwitcher -> {
            TabSwitcherScreen(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = { viewModel.switchTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { isIncognito -> viewModel.openNewTab(isIncognito = isIncognito) },
                onRestoreTab = { viewModel.restoreClosedTab() },
                onBack = { viewModel.closeScreen() }
            )
        }
        is BrowserScreen.SecurityDashboard -> {
            SecurityDashboardScreen(
                threatsBlocked = totalThreatsBlocked,
                malwarePhishingBlocked = malwarePhishingBlocked,
                adsBlocked = adsBlocked,
                trackersBlocked = trackersBlocked,
                islamicSafeBlocked = islamicSafeBlocked,
                recentEvents = recentEvents,
                settings = settings,
                dnsState = dnsState,
                onBack = { viewModel.closeScreen() },
                onRefreshDns = { viewModel.checkDnsStatus() },
                kahafDnsResolver = viewModel.kahafDnsResolver
            )
        }
        is BrowserScreen.IslamicSafeMode -> {
            IslamicSafeModeScreen(
                settings = settings,
                userRules = userRules,
                kahafGuardEngine = viewModel.kahafGuardEngine,
                onUpdateSettings = { viewModel.updateSettings(it) },
                onBack = { viewModel.closeScreen() }
            )
        }
        is BrowserScreen.Settings -> {
            SettingsScreen(
                settings = settings,
                dnsState = dnsState,
                onUpdateSettings = { viewModel.updateSettings(it) },
                onClearBrowsingData = { viewModel.clearBrowsingData(context, true, true, true) },
                onOpenIslamicSafeMode = { viewModel.openScreen(BrowserScreen.IslamicSafeMode) },
                onOpenSecurityDashboard = { viewModel.openScreen(BrowserScreen.SecurityDashboard) },
                onBack = { viewModel.closeScreen() },
                kahafDnsResolver = viewModel.kahafDnsResolver
            )
        }
        is BrowserScreen.History -> {
            HistoryScreen(
                historyList = historyList,
                onSelectUrl = {
                    viewModel.navigate(it)
                    viewModel.closeScreen()
                },
                onClearHistory = { viewModel.clearHistory() },
                onBack = { viewModel.closeScreen() }
            )
        }
        is BrowserScreen.Bookmarks -> {
            BookmarksScreen(
                bookmarksList = bookmarksList,
                onSelectUrl = {
                    viewModel.navigate(it)
                    viewModel.closeScreen()
                },
                onDeleteBookmark = { viewModel.removeBookmark(it) },
                onBack = { viewModel.closeScreen() }
            )
        }
        is BrowserScreen.Downloads -> {
            DownloadsScreen(
                downloads = downloadsList,
                onBack = { viewModel.closeScreen() }
            )
        }
        is BrowserScreen.Browser -> {
            Scaffold(
                topBar = {
                    TopSearchBar(
                        tab = activeTab,
                        tabCount = tabs.size,
                        urlInput = urlInput,
                        onUrlInputChange = { viewModel.setUrlInput(it) },
                        onNavigate = { viewModel.navigate(it) },
                        onReload = { viewModel.reload() },
                        onOpenTabs = { viewModel.openScreen(BrowserScreen.TabSwitcher) },
                        onOpenSecurityInspector = { viewModel.toggleSecurityInspector(true) },
                        onOpenSecurityDashboard = { viewModel.openScreen(BrowserScreen.SecurityDashboard) },
                        onOpenIslamicSafeMode = { viewModel.openScreen(BrowserScreen.IslamicSafeMode) },
                        onOpenBookmarks = { viewModel.openScreen(BrowserScreen.Bookmarks) },
                        onOpenHistory = { viewModel.openScreen(BrowserScreen.History) },
                        onOpenDownloads = { viewModel.openScreen(BrowserScreen.Downloads) },
                        onOpenSettings = { viewModel.openScreen(BrowserScreen.Settings) },
                        onToggleBookmark = { viewModel.addBookmark() },
                        isBookmarked = bookmarksList.any { it.url == activeTab?.url },
                        onNewTab = { viewModel.openNewTab() },
                        onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                        onClearBrowsingData = { viewModel.clearBrowsingData(context, true, true, true) },
                        isDesktopMode = settings.desktopMode,
                        onToggleDesktopMode = { viewModel.toggleDesktopMode() }
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        canGoBack = activeTab?.canGoBack == true || activeTab?.isBlocked == true,
                        canGoForward = activeTab?.canGoForward == true,
                        onBack = { viewModel.goBack() },
                        onForward = { viewModel.goForward() },
                        onHome = { viewModel.goHome() },
                        onOpenTabs = { viewModel.openScreen(BrowserScreen.TabSwitcher) },
                        onOpenSecurityDashboard = { viewModel.openScreen(BrowserScreen.SecurityDashboard) },
                        onOpenIslamicSafeMode = { viewModel.openScreen(BrowserScreen.IslamicSafeMode) }
                    )
                },
                modifier = modifier
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    val currentTab = activeTab
                    if (currentTab != null) {
                        if (currentTab.isBlocked) {
                            BlockedWarningScreen(
                                tab = currentTab,
                                onGoBack = { viewModel.goBack() },
                                onAllowOnce = { viewModel.allowBlockedUrlOnce() },
                                onReportIncorrectBlock = { /* feedback registered */ }
                            )
                        } else if (currentTab.url == "about:blank" || currentTab.url.isEmpty()) {
                            BrowserHomeScreen(
                                settings = settings,
                                dnsState = dnsState,
                                threatsBlocked = totalThreatsBlocked,
                                adsBlocked = adsBlocked,
                                trackersBlocked = trackersBlocked,
                                onNavigate = { viewModel.navigate(it) },
                                onOpenSecurityDashboard = { viewModel.openScreen(BrowserScreen.SecurityDashboard) }
                            )
                        } else {
                            RMWebViewContainer(
                                tabId = currentTab.id,
                                urlToLoad = currentTab.url,
                                settings = settings,
                                kahafGuardEngine = viewModel.kahafGuardEngine,
                                downloadManager = viewModel.downloadManager,
                                isIncognito = currentTab.isIncognito,
                                onPageStarted = { url -> viewModel.onPageStarted(currentTab.id, url) },
                                onPageFinished = { url -> viewModel.onPageFinished(currentTab.id, url) },
                                onProgressChanged = { progress -> viewModel.onProgressChanged(currentTab.id, progress) },
                                onTitleReceived = { title -> viewModel.onTitleReceived(currentTab.id, title) },
                                onUrlBlocked = { eval -> viewModel.onUrlBlocked(currentTab.id, eval) },
                                onSslError = { url, err -> viewModel.onSslError(currentTab.id, url, err) },
                                onError = { _, _ -> /* error state handled */ },
                                onRegisterWebView = { wv -> viewModel.registerWebView(currentTab.id, wv) },
                                onShowCustomView = { v, cb ->
                                    customVideoView = v
                                    customVideoCallback = cb
                                },
                                onHideCustomView = {
                                    customVideoView = null
                                    customVideoCallback = null
                                },
                                onShowFileChooser = onShowFileChooser,
                                onDangerousDownloadPrompt = { name, callback ->
                                    viewModel.promptDangerousDownload(name, callback)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Security Inspector Bottom Sheet
    if (showSecurityInspector) {
        SecurityInspectorDialog(
            tab = activeTab,
            settings = settings,
            dnsState = dnsState,
            onDismiss = { viewModel.toggleSecurityInspector(false) },
            onOpenSecurityDashboard = {
                viewModel.toggleSecurityInspector(false)
                viewModel.openScreen(BrowserScreen.SecurityDashboard)
            }
        )
    }

    // Dangerous Download Warning Dialog
    val pending = pendingDownload
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDangerousDownload() },
            title = { Text("⚠️ Dangerous Executable Detected") },
            text = {
                Text("The file '${pending.first}' has an executable extension that may execute code on your device. RM Browser cyber shield recommends extreme caution.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val callback = pending.second
                        viewModel.dismissDangerousDownload()
                        callback()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatRed)
                ) {
                    Text("Download Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDangerousDownload() }) {
                    Text("Cancel (Safe)")
                }
            }
        )
    }
}
