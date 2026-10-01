package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserSettings
import com.example.data.model.SearchEngine
import com.example.data.model.ThemeMode
import com.example.dns.DnsSecurityState
import com.example.dns.KahafDnsResolver
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ThreatRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    dnsState: DnsSecurityState,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onClearBrowsingData: () -> Unit,
    onOpenIslamicSafeMode: () -> Unit,
    onOpenSecurityDashboard: () -> Unit,
    onBack: () -> Unit,
    kahafDnsResolver: KahafDnsResolver,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Engine Section
            item {
                SettingsCard(title = "SEARCH ENGINE", icon = Icons.Default.Search) {
                    SearchEngine.values().forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateSettings(settings.copy(searchEngine = engine)) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine,
                                onClick = { onUpdateSettings(settings.copy(searchEngine = engine)) },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = engine.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (settings.searchEngine == engine) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Appearance & Dark Mode
            item {
                SettingsCard(title = "APPEARANCE & THEME", icon = Icons.Default.Palette) {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateSettings(settings.copy(themeMode = mode)) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = { onUpdateSettings(settings.copy(themeMode = mode)) },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "⚙️ System Default"
                                    ThemeMode.DARK -> "🌙 Dark Mode"
                                    ThemeMode.LIGHT -> "☀️ Light Mode"
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Kahaf Guard DNS & Security Configuration
            item {
                SettingsCard(title = "KAHAF GUARD DNS", icon = Icons.Default.Dns) {
                    Text(
                        text = "Browser-Level DoH Protection: ACTIVE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RM Browser routes domain inquiries through Kahaf Guard to eliminate ads, malware, and haram web material.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "high.kahfguard.com",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Kahaf Guard DNS", "high.kahfguard.com")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied high.kahfguard.com to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy DNS Host")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { kahafDnsResolver.openSystemPrivateDnsSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Device-Wide Private DNS")
                    }
                }
            }

            // Privacy & Ad Blocking
            item {
                SettingsCard(title = "PRIVACY & CYBER GUARDS", icon = Icons.Default.PrivacyTip) {
                    SettingToggleRow(
                        title = "Block Advertisements",
                        subtitle = "Suppress intrusive banners, popups, and ad networks",
                        checked = settings.blockAds,
                        onCheckedChange = { onUpdateSettings(settings.copy(blockAds = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingToggleRow(
                        title = "Block Cross-Site Trackers",
                        subtitle = "Prevent surveillance analytics and fingerprinting scripts",
                        checked = settings.blockTrackers,
                        onCheckedChange = { onUpdateSettings(settings.copy(blockTrackers = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingToggleRow(
                        title = "Do Not Track (DNT) Header",
                        subtitle = "Transmit strict privacy preferences to servers",
                        checked = settings.doNotTrack,
                        onCheckedChange = { onUpdateSettings(settings.copy(doNotTrack = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingToggleRow(
                        title = "AI Internet Safety Layer",
                        subtitle = "Evaluate URLs with semantic on-device heuristics & AI",
                        checked = settings.aiFilterEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(aiFilterEnabled = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatRed.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ThreatRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Browsing History & Cookies", color = ThreatRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Access to Features
            item {
                SettingsCard(title = "FEATURE SHORTCUTS", icon = Icons.Default.Security) {
                    OutlinedButton(
                        onClick = onOpenIslamicSafeMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mosque, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Islamic Safe Mode Settings")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenSecurityDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Full Security Dashboard")
                    }
                }
            }

            // About RM Browser
            item {
                SettingsCard(title = "ABOUT RM BROWSER", icon = Icons.Default.Security) {
                    Text(
                        text = "RM Browser v1.0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "“RM Browser – Safe, Smart, Halal Internet”",
                        fontSize = 13.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Powered by Android Chromium WebView, Kahaf Guard DNS security engine, and family-safe AI filters.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Browsing Data?") },
            text = { Text("This will delete your browsing history, cached web files, and saved session cookies.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBrowsingData()
                        showClearDataDialog = false
                        Toast.makeText(context, "Browsing data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldPrimary,
                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
            )
        )
    }
}
