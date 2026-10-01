package com.example.security

enum class ThreatType(val label: String, val category: String) {
    SAFE("Safe", "Legitimate"),
    PHISHING("Phishing", "Cyber Threat"),
    MALWARE("Malware Domain", "Cyber Threat"),
    SCAM("Scam Website", "Cyber Threat"),
    ADULT("Adult Content", "Islamic Safe Mode"),
    GAMBLING("Gambling & Betting", "Islamic Safe Mode"),
    DRUGS("Drugs & Narcotics", "Islamic Safe Mode"),
    VIOLENCE("Violence & Extremism", "Islamic Safe Mode"),
    AD("Advertisement", "Ad Blocker"),
    TRACKER("Tracking & Telemetry", "Privacy Protection"),
    USER_BLOCK("User Blocklist", "Custom Filter"),
    SUSPICIOUS("Suspicious Pattern", "Heuristic AI")
}

data class SecurityEvaluationResult(
    val url: String,
    val domain: String,
    val isBlocked: Boolean,
    val threatType: ThreatType,
    val reason: String,
    val riskScore: Int, // 0 - 100
    val isProtectedByKahafGuard: Boolean = true
) {
    companion object {
        fun safe(url: String, domain: String) = SecurityEvaluationResult(
            url = url,
            domain = domain,
            isBlocked = false,
            threatType = ThreatType.SAFE,
            reason = "Verified safe under RM Browser & Kahaf Guard policies.",
            riskScore = 0,
            isProtectedByKahafGuard = true
        )

        fun blocked(
            url: String,
            domain: String,
            threatType: ThreatType,
            reason: String,
            riskScore: Int = 90
        ) = SecurityEvaluationResult(
            url = url,
            domain = domain,
            isBlocked = true,
            threatType = threatType,
            reason = reason,
            riskScore = riskScore,
            isProtectedByKahafGuard = true
        )
    }
}
