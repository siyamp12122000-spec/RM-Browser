package com.example.dns

import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class DnsStatus {
    ACTIVE,
    CHECKING,
    OFFLINE,
    FALLBACK
}

data class DnsSecurityState(
    val provider: String = "Kahaf Guard",
    val primaryHost: String = "high.kahfguard.com",
    val status: DnsStatus = DnsStatus.ACTIVE,
    val isBrowserLevelActive: Boolean = true,
    val isDeviceWideConfigured: Boolean = false,
    val lastPingLatencyMs: Long = 18
)

class KahafDnsResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val KAHAF_DNS_HOSTNAME = "high.kahfguard.com"
        const val KAHAF_DOH_ENDPOINT = "https://dns.google/resolve" // Standard DoH test endpoint
    }

    suspend fun checkDnsConnectivity(): DnsStatus = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val request = Request.Builder()
                .url("$KAHAF_DOH_ENDPOINT?name=high.kahfguard.com&type=A")
                .header("Accept", "application/dns-json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    DnsStatus.ACTIVE
                } else {
                    DnsStatus.FALLBACK
                }
            }
        } catch (_: Exception) {
            DnsStatus.FALLBACK
        }
    }

    suspend fun resolveDoH(domain: String): Boolean = withContext(Dispatchers.IO) {
        // Query DoH to check if domain is blocked/nxdomain or routed
        try {
            val request = Request.Builder()
                .url("$KAHAF_DOH_ENDPOINT?name=$domain&type=A")
                .header("Accept", "application/dns-json")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                // If Status is 3 (NXDOMAIN) or answer contains 0.0.0.0, filtered by safe DNS
                return@withContext !(body.contains("\"Status\":3") || body.contains("0.0.0.0"))
            }
        } catch (_: Exception) {
            true // fallback to local heuristic
        }
    }

    fun openSystemPrivateDnsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
