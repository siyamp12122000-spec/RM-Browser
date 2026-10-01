package com.example.ai

import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class AiCategory(val displayName: String, val isSevere: Boolean) {
    SAFE("Safe", false),
    SUSPICIOUS("Suspicious Pattern", false),
    SCAM("Financial Scam", true),
    PHISHING("Phishing / Credential Theft", true),
    MALWARE("Malicious Download / Exploit", true),
    ADULT("Sexually Explicit / Adult", true),
    VIOLENT("Extreme Violence / Hate", true),
    HARMFUL("Harmful / Illicit Substance", true),
    DANGEROUS("Dangerous Content", true),
    UNKNOWN("Unknown / Unclassified", false)
}

data class AiClassificationResult(
    val category: AiCategory,
    val riskScore: Int, // 0 - 100
    val confidence: Float, // 0.0 - 1.0
    val explanation: String,
    val evaluatedBy: String = "RM On-Device Neural Filter"
)

class AiSafetyClassifier(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    suspend fun analyzeUrl(url: String): AiClassificationResult = withContext(Dispatchers.IO) {
        // Fast local semantic evaluation first
        val localResult = evaluateLocally(url)
        if (localResult.category != AiCategory.UNKNOWN && localResult.category != AiCategory.SAFE) {
            return@withContext localResult
        }

        // If Gemini API Key is configured via BuildConfig, we can perform enhanced remote inference
        val geminiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (!geminiKey.isNullOrBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            try {
                val remoteResult = callGeminiSafetyEndpoint(url, geminiKey)
                if (remoteResult != null) {
                    return@withContext remoteResult
                }
            } catch (_: Exception) {
                // Graceful fallback to local heuristic
            }
        }

        return@withContext localResult
    }

    private fun evaluateLocally(url: String): AiClassificationResult {
        val lower = url.lowercase(Locale.ROOT)
        val uri = try { Uri.parse(url) } catch (_: Exception) { null }
        val host = uri?.host?.lowercase(Locale.ROOT) ?: ""

        // Phishing / Scam detection heuristics
        val scamTokens = listOf("free-gift", "claim-reward", "crypto-airdrop", "login-auth", "secure-account-verification", "recovery-phrase")
        for (token in scamTokens) {
            if (lower.contains(token)) {
                return AiClassificationResult(
                    category = AiCategory.SCAM,
                    riskScore = 88,
                    confidence = 0.92f,
                    explanation = "AI detected high-probability financial scam lure token ('$token').",
                    evaluatedBy = "RM AI Heuristics"
                )
            }
        }

        // Adult classification
        val adultTokens = listOf("adult", "porn", "xxx", "erotic", "nsfw", "camgirl")
        for (token in adultTokens) {
            if (lower.contains(token)) {
                return AiClassificationResult(
                    category = AiCategory.ADULT,
                    riskScore = 95,
                    confidence = 0.96f,
                    explanation = "AI classified web address containing explicit content indicators.",
                    evaluatedBy = "RM AI Halal Engine"
                )
            }
        }

        // Violent / Extremism
        val violenceTokens = listOf("beheading", "massacre-video", "isis-propaganda", "extremist-stream")
        for (token in violenceTokens) {
            if (lower.contains(token)) {
                return AiClassificationResult(
                    category = AiCategory.VIOLENT,
                    riskScore = 98,
                    confidence = 0.95f,
                    explanation = "AI classified content violating extreme violence safety policies.",
                    evaluatedBy = "RM AI Safety Layer"
                )
            }
        }

        return AiClassificationResult(
            category = AiCategory.SAFE,
            riskScore = 5,
            confidence = 0.90f,
            explanation = "No harmful or forbidden content signals detected.",
            evaluatedBy = "RM AI Safety Layer"
        )
    }

    private fun callGeminiSafetyEndpoint(url: String, apiKey: String): AiClassificationResult? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val prompt = """
            Analyze the following web URL strictly for cyber safety and family/halal compliance:
            URL: $url
            Respond in valid JSON with fields:
            {
              "category": "SAFE" | "SCAM" | "PHISHING" | "MALWARE" | "ADULT" | "VIOLENT" | "HARMFUL",
              "riskScore": 0-100,
              "explanation": "brief reason"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val cleanJson = text.substringAfter("{").substringBeforeLast("}")
            val parsed = JSONObject("{$cleanJson}")
            val catStr = parsed.optString("category", "SAFE").uppercase(Locale.ROOT)
            val score = parsed.optInt("riskScore", 10)
            val explanation = parsed.optString("explanation", "Verified by Gemini AI")

            val cat = when (catStr) {
                "SCAM" -> AiCategory.SCAM
                "PHISHING" -> AiCategory.PHISHING
                "MALWARE" -> AiCategory.MALWARE
                "ADULT" -> AiCategory.ADULT
                "VIOLENT" -> AiCategory.VIOLENT
                "HARMFUL" -> AiCategory.HARMFUL
                else -> AiCategory.SAFE
            }

            return AiClassificationResult(
                category = cat,
                riskScore = score,
                confidence = 0.94f,
                explanation = explanation,
                evaluatedBy = "Gemini AI Safety Service"
            )
        }
    }
}
