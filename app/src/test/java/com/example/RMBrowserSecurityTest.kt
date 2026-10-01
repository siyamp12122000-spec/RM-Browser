package com.example

import com.example.ai.AiSafetyClassifier
import com.example.data.model.BrowserSettings
import com.example.data.model.IslamicModeLevel
import com.example.download.BrowserDownloadManager
import com.example.security.KahafGuardEngine
import com.example.security.ThreatType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RMBrowserSecurityTest {

    private val engine = KahafGuardEngine()

    @Test
    fun testSafeUrlEvaluation() = runBlocking {
        val settings = BrowserSettings()
        val result = engine.evaluateUrl("https://en.wikipedia.org", settings)
        assertFalse("Wikipedia should not be blocked", result.isBlocked)
        assertEquals(ThreatType.SAFE, result.threatType)
    }

    @Test
    fun testPhishingDetection() = runBlocking {
        val settings = BrowserSettings(blockMalwarePhishing = true)
        val result = engine.evaluateUrl("https://paypal-secure-login-account.com", settings)
        assertTrue("Known phishing lure should be blocked", result.isBlocked)
        assertEquals(ThreatType.PHISHING, result.threatType)
    }

    @Test
    fun testAdultContentBlockedInIslamicSafeMode() = runBlocking {
        val settings = BrowserSettings(
            islamicSafeMode = IslamicModeLevel.STANDARD,
            blockAdult = true
        )
        val result = engine.evaluateUrl("https://pornhub.com/video123", settings)
        assertTrue("Adult website must be blocked in Islamic Safe Mode", result.isBlocked)
        assertEquals(ThreatType.ADULT, result.threatType)
    }

    @Test
    fun testGamblingBlockedInIslamicSafeMode() = runBlocking {
        val settings = BrowserSettings(
            islamicSafeMode = IslamicModeLevel.STANDARD,
            blockGambling = true
        )
        val result = engine.evaluateUrl("https://bet365.com", settings)
        assertTrue("Gambling platform must be blocked", result.isBlocked)
        assertEquals(ThreatType.GAMBLING, result.threatType)
    }

    @Test
    fun testUserAllowlistOverridesCategoryBlock() = runBlocking {
        val settings = BrowserSettings(
            islamicSafeMode = IslamicModeLevel.STANDARD,
            blockAdult = true
        )
        // Add domain to user allowlist
        engine.addUserAllow("custom-educational-safe.com")
        val result = engine.evaluateUrl("https://custom-educational-safe.com", settings)
        assertFalse("Allowed domain should be permitted", result.isBlocked)
    }

    @Test
    fun testUserBlocklistForcesBlock() = runBlocking {
        val settings = BrowserSettings(islamicSafeMode = IslamicModeLevel.OFF)
        engine.addUserBlock("distracting-site.com")
        val result = engine.evaluateUrl("https://distracting-site.com", settings)
        assertTrue("User blocklist domain must be blocked", result.isBlocked)
        assertEquals(ThreatType.USER_BLOCK, result.threatType)
    }

    @Test
    fun testAllowOnceTemporarySessionException() = runBlocking {
        val settings = BrowserSettings(
            islamicSafeMode = IslamicModeLevel.STANDARD,
            blockAdult = true
        )
        val domain = "temp-flagged-domain.com"
        engine.addUserBlock(domain)

        // Verify initially blocked
        val initial = engine.evaluateUrl("https://$domain", settings)
        assertTrue(initial.isBlocked)

        // Now simulate user tapping "Allow Once"
        engine.allowDomainOnce(domain)
        val afterAllowOnce = engine.evaluateUrl("https://$domain", settings)
        assertFalse("Domain granted temporary exception should be allowed", afterAllowOnce.isBlocked)
    }

    @Test
    fun testDangerousDownloadExtensionFilter() {
        val context = RuntimeEnvironment.getApplication()
        val downloadManager = BrowserDownloadManager(context)

        assertTrue(downloadManager.isDangerousFile("trojan.apk"))
        assertTrue(downloadManager.isDangerousFile("malware.exe"))
        assertTrue(downloadManager.isDangerousFile("script.bat"))
        assertTrue(downloadManager.isDangerousFile("installer.msi"))

        assertFalse(downloadManager.isDangerousFile("document.pdf"))
        assertFalse(downloadManager.isDangerousFile("photo.jpg"))
        assertFalse(downloadManager.isDangerousFile("book.epub"))
    }

    @Test
    fun testAiSafetyClassifierLocalEvaluation() = runBlocking {
        val classifier = AiSafetyClassifier()
        val scamResult = classifier.analyzeUrl("https://claim-reward-crypto-airdrop.xyz")
        assertTrue("Scam URL structure should be flagged by AI classifier", scamResult.riskScore > 50)
        assertNotNull(scamResult.explanation)
    }
}
