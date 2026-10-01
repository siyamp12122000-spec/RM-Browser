package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.model.BookmarkItem
import com.example.dns.KahafDnsResolver
import com.example.download.BrowserDownloadManager
import com.example.security.KahafGuardEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RMBrowserApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var kahafGuardEngine: KahafGuardEngine
        private set

    lateinit var kahafDnsResolver: KahafDnsResolver
        private set

    lateinit var downloadManager: BrowserDownloadManager
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        kahafGuardEngine = KahafGuardEngine(database.securityDao())
        kahafDnsResolver = KahafDnsResolver()
        downloadManager = BrowserDownloadManager(this)

        // Seed default safe starter bookmarks if empty
        applicationScope.launch {
            try {
                val current = database.bookmarkDao().getAllBookmarks().first()
                if (current.isEmpty()) {
                    database.bookmarkDao().insert(
                        BookmarkItem(
                            title = "Quran.com",
                            url = "https://quran.com",
                            category = "Islamic Learning"
                        )
                    )
                    database.bookmarkDao().insert(
                        BookmarkItem(
                            title = "Sunnah.com (Hadith)",
                            url = "https://sunnah.com",
                            category = "Islamic Learning"
                        )
                    )
                    database.bookmarkDao().insert(
                        BookmarkItem(
                            title = "IslamicFinder (Prayer Times)",
                            url = "https://www.islamicfinder.org",
                            category = "Islamic Tools"
                        )
                    )
                    database.bookmarkDao().insert(
                        BookmarkItem(
                            title = "Wikipedia",
                            url = "https://en.wikipedia.org",
                            category = "Knowledge"
                        )
                    )
                    database.bookmarkDao().insert(
                        BookmarkItem(
                            title = "DuckDuckGo Privacy Search",
                            url = "https://duckduckgo.com",
                            category = "Search"
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore seeding errors
            }
        }
    }

    companion object {
        lateinit var instance: RMBrowserApp
            private set
    }
}
