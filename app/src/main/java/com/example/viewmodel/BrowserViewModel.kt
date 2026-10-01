package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiSafetyClassifier
import com.example.data.local.AppDatabase
import com.example.data.model.BookmarkItem
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import com.example.data.model.FilterRule
import com.example.data.model.HistoryItem
import com.example.data.model.IslamicModeLevel
import com.example.data.model.SearchEngine
import com.example.data.model.SecurityEvent
import com.example.data.model.ThemeMode
import com.example.dns.DnsSecurityState
import com.example.dns.DnsStatus
import com.example.dns.KahafDnsResolver
import com.example.download.BrowserDownloadManager
import com.example.security.KahafGuardEngine
import com.example.security.SecurityEvaluationResult
import com.example.security.ThreatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Stack
import java.util.UUID

sealed class BrowserScreen {
    object Browser : BrowserScreen()
    object TabSwitcher : BrowserScreen()
    object SecurityDashboard : BrowserScreen()
    object IslamicSafeMode : BrowserScreen()
    object Settings : BrowserScreen()
    object History : BrowserScreen()
    object Bookmarks : BrowserScreen()
    object Downloads : BrowserScreen()
}

class BrowserViewModel(
    private val database: AppDatabase,
    val kahafGuardEngine: KahafGuardEngine,
    val kahafDnsResolver: KahafDnsResolver,
    val downloadManager: BrowserDownloadManager
) : ViewModel() {

    private val aiClassifier = AiSafetyClassifier()

    // Screen navigation
    private val _currentScreen = MutableStateFlow<BrowserScreen>(BrowserScreen.Browser)
    val currentScreen: StateFlow<BrowserScreen> = _currentScreen.asStateFlow()

    // Tabs
    private val initialTabId = UUID.randomUUID().toString()
    private val _tabs = MutableStateFlow<List<BrowserTab>>(
        listOf(BrowserTab(id = initialTabId, title = "New Tab", url = "about:blank"))
    )
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTabId)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Closed tabs stack for "Restore Tab"
    private val closedTabsStack = Stack<BrowserTab>()

    // Current active tab
    val activeTab: StateFlow<BrowserTab?> = combine(_tabs, _activeTabId) { tabsList, activeId ->
        tabsList.find { it.id == activeId } ?: tabsList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Address bar query text
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    // Browser settings
    private val _settings = MutableStateFlow(BrowserSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    // DNS security state
    private val _dnsState = MutableStateFlow(DnsSecurityState())
    val dnsState: StateFlow<DnsSecurityState> = _dnsState.asStateFlow()

    // Active WebView reference map
    private val webViewMap = mutableMapOf<String, WebView>()

    // History and Bookmarks flows from Room
    val historyList: StateFlow<List<HistoryItem>> = database.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksList: StateFlow<List<BookmarkItem>> = database.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Security Statistics from Room
    val totalThreatsBlocked: StateFlow<Int> = database.securityDao().getTotalThreatsBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val malwarePhishingBlocked: StateFlow<Int> = database.securityDao().getMalwarePhishingBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val adsBlocked: StateFlow<Int> = database.securityDao().getAdsBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trackersBlocked: StateFlow<Int> = database.securityDao().getTrackersBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val islamicSafeBlocked: StateFlow<Int> = database.securityDao().getIslamicSafeBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentSecurityEvents: StateFlow<List<SecurityEvent>> = database.securityDao().getRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userFilterRules: StateFlow<List<FilterRule>> = database.securityDao().getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dangerous download confirmation dialog state
    private val _pendingDownload = MutableStateFlow<Pair<String, () -> Unit>?>(null)
    val pendingDownload: StateFlow<Pair<String, () -> Unit>?> = _pendingDownload.asStateFlow()

    // Security Inspector BottomSheet dialog
    private val _showSecurityInspector = MutableStateFlow(false)
    val showSecurityInspector: StateFlow<Boolean> = _showSecurityInspector.asStateFlow()

    init {
        checkDnsStatus()
    }

    fun openScreen(screen: BrowserScreen) {
        _currentScreen.value = screen
    }

    fun closeScreen() {
        _currentScreen.value = BrowserScreen.Browser
    }

    fun setUrlInput(input: String) {
        _urlInput.value = input
    }

    fun registerWebView(tabId: String, webView: WebView) {
        webViewMap[tabId] = webView
    }

    fun checkDnsStatus() {
        viewModelScope.launch {
            _dnsState.value = _dnsState.value.copy(status = DnsStatus.CHECKING)
            val result = kahafDnsResolver.checkDnsConnectivity()
            _dnsState.value = _dnsState.value.copy(status = result)
        }
    }

    fun openNewTab(url: String = "about:blank", isIncognito: Boolean = false) {
        val newId = UUID.randomUUID().toString()
        val newTab = BrowserTab(
            id = newId,
            title = if (url == "about:blank") "New Tab" else "Loading...",
            url = url,
            isIncognito = isIncognito
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newId
        _urlInput.value = if (url == "about:blank") "" else url
        _currentScreen.value = BrowserScreen.Browser
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        val tabToClose = currentList.find { it.id == tabId }
        if (tabToClose != null && !tabToClose.isIncognito) {
            closedTabsStack.push(tabToClose)
        }

        webViewMap.remove(tabId)?.apply {
            stopLoading()
            destroy()
        }

        if (currentList.size <= 1) {
            // Keep at least one tab open
            val newId = UUID.randomUUID().toString()
            val replacement = BrowserTab(id = newId, title = "New Tab", url = "about:blank")
            _tabs.value = listOf(replacement)
            _activeTabId.value = newId
            _urlInput.value = ""
        } else {
            val newList = currentList.filter { it.id != tabId }
            _tabs.value = newList
            if (_activeTabId.value == tabId) {
                val nextActive = newList.last()
                _activeTabId.value = nextActive.id
                _urlInput.value = if (nextActive.url == "about:blank") "" else nextActive.url
            }
        }
    }

    fun restoreClosedTab() {
        if (!closedTabsStack.isEmpty()) {
            val restored = closedTabsStack.pop()
            _tabs.value = _tabs.value + restored
            _activeTabId.value = restored.id
            _urlInput.value = if (restored.url == "about:blank") "" else restored.url
            _currentScreen.value = BrowserScreen.Browser
        }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
        val tab = _tabs.value.find { it.id == tabId }
        _urlInput.value = if (tab?.url == "about:blank") "" else (tab?.url ?: "")
        _currentScreen.value = BrowserScreen.Browser
    }

    fun navigate(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = formatUrlOrSearch(trimmed)
        val activeId = _activeTabId.value

        // Pre-navigation check
        viewModelScope.launch {
            val eval = kahafGuardEngine.evaluateUrl(targetUrl, _settings.value)
            if (eval.isBlocked) {
                updateTab(activeId) {
                    it.copy(
                        isBlocked = true,
                        blockedReason = eval.reason,
                        blockedThreatType = eval.threatType.label,
                        blockedUrl = targetUrl,
                        url = targetUrl,
                        title = "Blocked - RM Security"
                    )
                }
            } else {
                updateTab(activeId) {
                    it.copy(
                        url = targetUrl,
                        isBlocked = false,
                        blockedReason = null,
                        blockedThreatType = null,
                        blockedUrl = null,
                        title = "Loading...",
                        isLoading = true
                    )
                }
                _urlInput.value = targetUrl
                webViewMap[activeId]?.loadUrl(targetUrl)
            }
        }
    }

    private fun formatUrlOrSearch(query: String): String {
        val hasScheme = query.startsWith("http://", ignoreCase = true) ||
                query.startsWith("https://", ignoreCase = true) ||
                query.startsWith("about:", ignoreCase = true)

        if (hasScheme) return query

        // Check if looks like domain name (e.g., example.com, sub.example.org/path)
        val domainRegex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$")
        return if (domainRegex.matches(query) && !query.contains(" ")) {
            "https://$query"
        } else {
            val encoded = try {
                URLEncoder.encode(query, "UTF-8")
            } catch (_: Exception) {
                query
            }
            "${_settings.value.searchEngine.searchUrl}$encoded"
        }
    }

    fun reload() {
        val activeId = _activeTabId.value
        val tab = _tabs.value.find { it.id == activeId }
        if (tab?.isBlocked == true && tab.blockedUrl != null) {
            navigate(tab.blockedUrl)
        } else {
            webViewMap[activeId]?.reload()
        }
    }

    fun goBack(): Boolean {
        val activeId = _activeTabId.value
        val wv = webViewMap[activeId]
        val tab = _tabs.value.find { it.id == activeId }

        if (tab?.isBlocked == true) {
            // Dismiss blocked screen, return to blank/home
            updateTab(activeId) {
                it.copy(
                    isBlocked = false,
                    url = "about:blank",
                    title = "New Tab"
                )
            }
            _urlInput.value = ""
            return true
        }

        if (wv != null && wv.canGoBack()) {
            wv.goBack()
            return true
        }
        return false
    }

    fun goForward(): Boolean {
        val activeId = _activeTabId.value
        val wv = webViewMap[activeId]
        if (wv != null && wv.canGoForward()) {
            wv.goForward()
            return true
        }
        return false
    }

    fun goHome() {
        val activeId = _activeTabId.value
        updateTab(activeId) {
            it.copy(
                url = "about:blank",
                title = "New Tab",
                isBlocked = false,
                isLoading = false,
                progress = 0
            )
        }
        _urlInput.value = ""
        webViewMap[activeId]?.loadUrl("about:blank")
    }

    // Callbacks from WebView
    fun onPageStarted(tabId: String, url: String) {
        updateTab(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                isSslSecure = url.startsWith("https://", ignoreCase = true),
                canGoBack = webViewMap[tabId]?.canGoBack() ?: false,
                canGoForward = webViewMap[tabId]?.canGoForward() ?: false
            )
        }
        if (_activeTabId.value == tabId && url != "about:blank") {
            _urlInput.value = url
        }
    }

    fun onPageFinished(tabId: String, url: String) {
        val wv = webViewMap[tabId]
        val tab = _tabs.value.find { it.id == tabId }
        val title = wv?.title ?: tab?.title ?: "Web Page"

        updateTab(tabId) {
            it.copy(
                url = url,
                title = title,
                isLoading = false,
                progress = 100,
                canGoBack = wv?.canGoBack() ?: false,
                canGoForward = wv?.canGoForward() ?: false
            )
        }

        if (_activeTabId.value == tabId && url != "about:blank") {
            _urlInput.value = url
        }

        // Record history if not incognito
        if (tab?.isIncognito == false && url != "about:blank" && !url.startsWith("data:")) {
            viewModelScope.launch {
                try {
                    val existing = database.historyDao().findByUrl(url)
                    if (existing != null) {
                        database.historyDao().update(
                            existing.copy(
                                timestamp = System.currentTimeMillis(),
                                visitCount = existing.visitCount + 1,
                                title = title
                            )
                        )
                    } else {
                        database.historyDao().insert(
                            HistoryItem(
                                title = title,
                                url = url,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Ignore DB history recording issues
                }
            }
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun onTitleReceived(tabId: String, title: String) {
        updateTab(tabId) { it.copy(title = title) }
    }

    fun onUrlBlocked(tabId: String, eval: SecurityEvaluationResult) {
        updateTab(tabId) {
            it.copy(
                isBlocked = true,
                blockedReason = eval.reason,
                blockedThreatType = eval.threatType.label,
                blockedUrl = eval.url,
                title = "Blocked by RM Browser"
            )
        }
    }

    fun onSslError(tabId: String, url: String, errorDesc: String) {
        updateTab(tabId) {
            it.copy(
                isBlocked = true,
                blockedReason = "SSL Certificate Validation Failed: $errorDesc",
                blockedThreatType = "Untrusted SSL Connection",
                blockedUrl = url,
                isSslSecure = false,
                title = "SSL Security Warning"
            )
        }
    }

    fun allowBlockedUrlOnce() {
        val activeId = _activeTabId.value
        val tab = _tabs.value.find { it.id == activeId } ?: return
        val url = tab.blockedUrl ?: tab.url
        val domain = Uri.parse(url).host ?: ""

        if (domain.isNotEmpty()) {
            kahafGuardEngine.allowDomainOnce(domain)
            updateTab(activeId) {
                it.copy(
                    isBlocked = false,
                    blockedReason = null,
                    blockedThreatType = null
                )
            }
            webViewMap[activeId]?.loadUrl(url)
        }
    }

    fun addBookmark() {
        val tab = activeTab.value ?: return
        if (tab.url == "about:blank" || tab.url.isEmpty()) return

        viewModelScope.launch {
            database.bookmarkDao().insert(
                BookmarkItem(
                    title = tab.title,
                    url = tab.url
                )
            )
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            database.bookmarkDao().deleteByUrl(url)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.historyDao().clearAll()
        }
    }

    fun clearBrowsingData(context: Context, history: Boolean, cookies: Boolean, cache: Boolean) {
        viewModelScope.launch {
            if (history) {
                database.historyDao().clearAll()
            }
            if (cookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (cache) {
                webViewMap.values.forEach { it.clearCache(true) }
                WebStorage.getInstance().deleteAllData()
            }
        }
    }

    fun promptDangerousDownload(fileName: String, onConfirm: () -> Unit) {
        _pendingDownload.value = Pair(fileName, onConfirm)
    }

    fun dismissDangerousDownload() {
        _pendingDownload.value = null
    }

    fun toggleSecurityInspector(show: Boolean) {
        _showSecurityInspector.value = show
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _settings.value = newSettings
    }

    fun updateIslamicMode(level: IslamicModeLevel) {
        _settings.value = _settings.value.copy(islamicSafeMode = level)
    }

    fun toggleSearchEngine(engine: SearchEngine) {
        _settings.value = _settings.value.copy(searchEngine = engine)
    }

    fun toggleThemeMode(theme: ThemeMode) {
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun toggleDesktopMode() {
        val current = _settings.value.desktopMode
        _settings.value = _settings.value.copy(desktopMode = !current)
        reload()
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) transform(it) else it }
    }
}

class BrowserViewModelFactory(
    private val database: AppDatabase,
    private val kahafGuardEngine: KahafGuardEngine,
    private val kahafDnsResolver: KahafDnsResolver,
    private val downloadManager: BrowserDownloadManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BrowserViewModel(database, kahafGuardEngine, kahafDnsResolver, downloadManager) as T
    }
}
