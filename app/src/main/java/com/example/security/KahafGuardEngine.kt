package com.example.security

import android.net.Uri
import com.example.data.local.SecurityDao
import com.example.data.model.BrowserSettings
import com.example.data.model.IslamicModeLevel
import com.example.data.model.SecurityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class KahafGuardEngine(
    private val securityDao: SecurityDao? = null
) {
    // Temporary allowlist for current session ("Allow Once")
    private val sessionAllowList = ConcurrentHashMap.newKeySet<String>()

    // Custom user rules loaded from database
    private val userAllowList = ConcurrentHashMap.newKeySet<String>()
    private val userBlockList = ConcurrentHashMap.newKeySet<String>()

    // Maintained domain datasets
    private val adultDomains = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
        "chaturbate.com", "onlyfans.com", "stripchat.com", "cam4.com", "livejasmin.com",
        "brazzers.com", "erome.com", "spankbang.com", "eporner.com", "beeg.com",
        "tubegalore.com", "hqporner.com", "xhamster.com", "bongaCams.com", "adultfriendfinder.com",
        "playboy.com", "penthouse.com", "fapello.com", "fetlife.com"
    )

    private val adultKeywords = listOf(
        "porn", "xxx", "erotic", "nude", "camgirl", "sexvideo",
        "nsfw", "escort", "hentai", "stripper"
    )

    private val gamblingDomains = setOf(
        "bet365.com", "1xbet.com", "bovada.lv", "draftkings.com", "fanduel.com",
        "pokerstars.com", "stake.com", "roobet.com", "betway.com", "888casino.com",
        "betonline.ag", "williamhill.com", "unibet.com", "betfair.com", "paddypower.com",
        "casinocruise.com", "spinpalace.com", "jackpotcity.com", "bwin.com"
    )

    private val gamblingKeywords = listOf(
        "casino", "betting", "poker", "roulette", "slotmachine",
        "jackpot", "sportsbook", "wager", "baccarat"
    )

    private val drugsAlcoholDomains = setOf(
        "silkroad.onion", "buyweedonline.com", "leafly.com", "weedmaps.com",
        "smokecartel.com", "totalwine.com", "drizly.com"
    )

    private val malwarePhishingDomains = setOf(
        "appleid-verify-alert.com", "paypal-secure-login-account.com",
        "chase-update-security.com", "wellsfargo-auth-online.com",
        "metamask-validation.io", "trustwallet-verify-seed.com",
        "binance-security-check.top", "login-microsoft-auth.xyz",
        "free-vbucks-giftcard.net", "steam-community-freegifts.ru",
        "telegram-gift-airdrop.org", "walletconnect-recovery.site"
    )

    private val adDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "criteo.com", "outbrain.com",
        "taboola.com", "popads.net", "propellerads.com", "adroll.com",
        "rubiconproject.com", "pubmatic.com", "openx.net", "inmobi.com",
        "unityads.unity3d.com", "ironsrc.mobi", "vungle.com", "applovin.com"
    )

    private val trackerDomains = setOf(
        "google-analytics.com", "hotjar.com", "scorecardresearch.com",
        "quantserve.com", "mixpanel.com", "segment.io", "amplitude.com",
        "facebook.net", "connect.facebook.net", "clarity.ms", "yandex.ru/metrika",
        "statcounter.com", "crazyegg.com"
    )

    fun allowDomainOnce(domain: String) {
        sessionAllowList.add(domain.lowercase(Locale.ROOT))
    }

    fun addUserAllow(domain: String) {
        userAllowList.add(domain.lowercase(Locale.ROOT))
        userBlockList.remove(domain.lowercase(Locale.ROOT))
    }

    fun addUserBlock(domain: String) {
        userBlockList.add(domain.lowercase(Locale.ROOT))
        userAllowList.remove(domain.lowercase(Locale.ROOT))
    }

    fun removeUserRule(domain: String) {
        val lower = domain.lowercase(Locale.ROOT)
        userAllowList.remove(lower)
        userBlockList.remove(lower)
    }

    suspend fun evaluateUrl(
        rawUrl: String,
        settings: BrowserSettings
    ): SecurityEvaluationResult = withContext(Dispatchers.Default) {
        val uri = try {
            Uri.parse(rawUrl)
        } catch (_: Exception) {
            return@withContext SecurityEvaluationResult.safe(rawUrl, "unknown")
        }

        val host = (uri.host ?: "").lowercase(Locale.ROOT).trim()
        val path = (uri.path ?: "").lowercase(Locale.ROOT)

        if (host.isEmpty() || host == "localhost" || host == "127.0.0.1") {
            return@withContext SecurityEvaluationResult.safe(rawUrl, host)
        }

        // 1. Session allowlist ("Allow Once")
        if (sessionAllowList.contains(host) || isSubdomainOfAny(host, sessionAllowList)) {
            return@withContext SecurityEvaluationResult.safe(rawUrl, host)
        }

        // 2. User custom allowlist
        if (userAllowList.contains(host) || isSubdomainOfAny(host, userAllowList)) {
            return@withContext SecurityEvaluationResult.safe(rawUrl, host)
        }

        // 3. User custom blocklist
        if (userBlockList.contains(host) || isSubdomainOfAny(host, userBlockList)) {
            val res = SecurityEvaluationResult.blocked(
                url = rawUrl,
                domain = host,
                threatType = ThreatType.USER_BLOCK,
                reason = "Blocked by your personal custom filter rule.",
                riskScore = 80
            )
            recordEvent(res)
            return@withContext res
        }

        // 4. Cyber Threats: Phishing & Malware (always blocked when enabled)
        if (settings.blockMalwarePhishing) {
            if (isMalwareOrPhishing(host, rawUrl)) {
                val res = SecurityEvaluationResult.blocked(
                    url = rawUrl,
                    domain = host,
                    threatType = ThreatType.PHISHING,
                    reason = "Known cyber threat or credential theft website blocked by Kahaf Guard reputation engine.",
                    riskScore = 98
                )
                recordEvent(res)
                return@withContext res
            }
        }

        // 5. Islamic Safe Mode Filtering
        if (settings.islamicSafeMode != IslamicModeLevel.OFF) {
            // Adult Content
            if (settings.blockAdult && isAdultContent(host, path, settings.islamicSafeMode)) {
                val res = SecurityEvaluationResult.blocked(
                    url = rawUrl,
                    domain = host,
                    threatType = ThreatType.ADULT,
                    reason = "Classified as adult or sexually explicit content under Kahaf Guard Halal Web rules.",
                    riskScore = 95
                )
                recordEvent(res)
                return@withContext res
            }

            // Gambling & Betting
            if (settings.blockGambling && isGambling(host, path)) {
                val res = SecurityEvaluationResult.blocked(
                    url = rawUrl,
                    domain = host,
                    threatType = ThreatType.GAMBLING,
                    reason = "Classified as gambling, sports betting, or lottery forbidden under Islamic Safe Mode.",
                    riskScore = 95
                )
                recordEvent(res)
                return@withContext res
            }

            // Drugs & Alcohol
            if (settings.blockDrugsAlcohol && isDrugsOrAlcohol(host, path)) {
                val res = SecurityEvaluationResult.blocked(
                    url = rawUrl,
                    domain = host,
                    threatType = ThreatType.DRUGS,
                    reason = "Classified as illicit substance or alcohol vendor blocked under Islamic Safe Mode.",
                    riskScore = 90
                )
                recordEvent(res)
                return@withContext res
            }
        }

        // 6. Ad & Tracker blocking (for full web page domain navigation)
        if (settings.blockAds && isAdDomain(host)) {
            val res = SecurityEvaluationResult.blocked(
                url = rawUrl,
                domain = host,
                threatType = ThreatType.AD,
                reason = "Advertising delivery network blocked by RM Browser ad guard.",
                riskScore = 60
            )
            recordEvent(res)
            return@withContext res
        }

        if (settings.blockTrackers && isTrackerDomain(host)) {
            val res = SecurityEvaluationResult.blocked(
                url = rawUrl,
                domain = host,
                threatType = ThreatType.TRACKER,
                reason = "Cross-site behavioral tracker blocked by RM Browser privacy shield.",
                riskScore = 65
            )
            recordEvent(res)
            return@withContext res
        }

        // 7. Heuristic suspicious pattern checks
        val suspiciousReason = checkHeuristics(host, rawUrl)
        if (suspiciousReason != null && settings.islamicSafeMode == IslamicModeLevel.STRICT) {
            val res = SecurityEvaluationResult.blocked(
                url = rawUrl,
                domain = host,
                threatType = ThreatType.SUSPICIOUS,
                reason = suspiciousReason,
                riskScore = 75
            )
            recordEvent(res)
            return@withContext res
        }

        // Safe
        return@withContext SecurityEvaluationResult.safe(rawUrl, host)
    }

    fun isAdOrTracker(domain: String): ThreatType? {
        val lower = domain.lowercase(Locale.ROOT)
        if (isAdDomain(lower)) return ThreatType.AD
        if (isTrackerDomain(lower)) return ThreatType.TRACKER
        return null
    }

    private fun isMalwareOrPhishing(host: String, rawUrl: String): Boolean {
        if (malwarePhishingDomains.contains(host) || isSubdomainOfAny(host, malwarePhishingDomains)) {
            return true
        }
        // Phishing heuristic: suspicious keyword stacking (e.g. paypal-security-update-account)
        val suspiciousKeywords = listOf("paypal", "appleid", "wellsfargo", "binance", "metamask", "coinbase")
        for (kw in suspiciousKeywords) {
            if (host.contains(kw) && !host.endsWith(".$kw.com") && host != "$kw.com") {
                if (host.contains("login") || host.contains("verify") || host.contains("secure") || host.contains("wallet")) {
                    return true
                }
            }
        }
        return false
    }

    private fun isAdultContent(host: String, path: String, level: IslamicModeLevel): Boolean {
        if (adultDomains.contains(host) || isSubdomainOfAny(host, adultDomains)) {
            return true
        }
        for (kw in adultKeywords) {
            if (host.contains(kw)) return true
        }
        if (level == IslamicModeLevel.STRICT) {
            val strictAdultWords = listOf("dating", "hookup", "bikini", "lingerie", "flirt")
            for (w in strictAdultWords) {
                if (host.contains(w) || path.contains(w)) return true
            }
        }
        return false
    }

    private fun isGambling(host: String, path: String): Boolean {
        if (gamblingDomains.contains(host) || isSubdomainOfAny(host, gamblingDomains)) {
            return true
        }
        for (kw in gamblingKeywords) {
            if (host.contains(kw)) return true
        }
        return false
    }

    private fun isDrugsOrAlcohol(host: String, path: String): Boolean {
        if (drugsAlcoholDomains.contains(host) || isSubdomainOfAny(host, drugsAlcoholDomains)) {
            return true
        }
        val drugWords = listOf("weed", "cannabis", "dispensary", "narcotic", "marijuana", "liquor", "beer", "wine")
        for (w in drugWords) {
            if (host.contains(w) && !host.contains("news") && !host.contains("wikipedia")) {
                return true
            }
        }
        return false
    }

    private fun isAdDomain(host: String): Boolean {
        return adDomains.contains(host) || isSubdomainOfAny(host, adDomains)
    }

    private fun isTrackerDomain(host: String): Boolean {
        return trackerDomains.contains(host) || isSubdomainOfAny(host, trackerDomains)
    }

    private fun checkHeuristics(host: String, rawUrl: String): String? {
        // High risk suspicious TLD with financial keywords
        val highRiskTlds = listOf(".top", ".xyz", ".bid", ".loan", ".racing", ".men", ".stream")
        val financialKw = listOf("bank", "crypto", "pay", "login", "secure", "auth", "claim", "free")
        for (tld in highRiskTlds) {
            if (host.endsWith(tld)) {
                for (kw in financialKw) {
                    if (host.contains(kw)) {
                        return "Heuristic alert: Suspicious top-level domain ($tld) combined with credential lure '$kw'."
                    }
                }
            }
        }
        // Direct raw IPv4 host with sensitive path
        if (host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
            if (rawUrl.contains("login") || rawUrl.contains("verify") || rawUrl.contains("auth")) {
                return "Heuristic alert: Direct IP web address requesting authentication credentials."
            }
        }
        return null
    }

    private fun isSubdomainOfAny(host: String, domainSet: Set<String>): Boolean {
        for (base in domainSet) {
            if (host.endsWith(".$base")) {
                return true
            }
        }
        return false
    }

    private suspend fun recordEvent(res: SecurityEvaluationResult) {
        securityDao?.let { dao ->
            try {
                dao.insertEvent(
                    SecurityEvent(
                        url = res.url,
                        domain = res.domain,
                        threatType = res.threatType.name,
                        actionTaken = if (res.isBlocked) "BLOCKED" else "ALLOWED",
                        reason = res.reason
                    )
                )
            } catch (_: Exception) {
                // Ignore DB logging issues
            }
        }
    }
}
