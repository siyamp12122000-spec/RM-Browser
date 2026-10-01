package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)

@Entity(tableName = "bookmarks")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_events")
data class SecurityEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String,
    val domain: String,
    val threatType: String,
    val actionTaken: String, // "BLOCKED", "WARNED", "ALLOWED"
    val reason: String
)

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val ruleType: String, // "ALLOW", "BLOCK"
    val category: String, // "MALWARE", "ADULT", "GAMBLING", "AD", "TRACKER", "CUSTOM"
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "about:blank",
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val favicon: String? = null,
    val isBlocked: Boolean = false,
    val blockedReason: String? = null,
    val blockedThreatType: String? = null,
    val isSslSecure: Boolean = true,
    val blockedUrl: String? = null
)

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo (Privacy)", "https://duckduckgo.com/?q="),
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    STARTPAGE("StartPage (Private)", "https://www.startpage.com/sp/search?query=")
}

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class IslamicModeLevel {
    OFF, STANDARD, STRICT
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val islamicSafeMode: IslamicModeLevel = IslamicModeLevel.STANDARD,
    val blockAdult: Boolean = true,
    val blockGambling: Boolean = true,
    val blockDrugsAlcohol: Boolean = true,
    val blockViolenceHate: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockMalwarePhishing: Boolean = true,
    val kahafGuardDnsEnabled: Boolean = true,
    val customDnsHost: String = "high.kahfguard.com",
    val aiFilterEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val doNotTrack: Boolean = true,
    val javascriptEnabled: Boolean = true
)
