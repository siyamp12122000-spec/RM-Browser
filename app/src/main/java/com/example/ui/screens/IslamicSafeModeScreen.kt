package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserSettings
import com.example.data.model.FilterRule
import com.example.data.model.IslamicModeLevel
import com.example.security.KahafGuardEngine
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGreen
import com.example.ui.theme.ThreatRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicSafeModeScreen(
    settings: BrowserSettings,
    userRules: List<FilterRule>,
    kahafGuardEngine: KahafGuardEngine,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newRuleDomain by remember { mutableStateOf("") }
    var newRuleType by remember { mutableStateOf("BLOCK") } // "BLOCK" or "ALLOW"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🕌 Islamic Safe Mode", fontWeight = FontWeight.Bold) },
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
            // Mode Selector: OFF / STANDARD / STRICT
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FILTERING SENSITIVITY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IslamicModeLevel.values().forEach { level ->
                                FilterChip(
                                    selected = settings.islamicSafeMode == level,
                                    onClick = {
                                        onUpdateSettings(settings.copy(islamicSafeMode = level))
                                    },
                                    label = {
                                        Text(
                                            text = level.name,
                                            fontWeight = if (settings.islamicSafeMode == level) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = GoldAccent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val modeDescription = when (settings.islamicSafeMode) {
                            IslamicModeLevel.OFF -> "Islamic Safe Mode is deactivated. Only malicious cyber threats and malware will be filtered."
                            IslamicModeLevel.STANDARD -> "Standard Halal Filtering: Automatically blocks adult websites, online gambling, narcotics, alcohol vendors, and phishing."
                            IslamicModeLevel.STRICT -> "Strict Halal Filtering: In addition to standard rules, blocks revealing/dating websites, high-risk unrated domains, and suspicious redirects."
                        }

                        Text(
                            text = modeDescription,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Categories configuration
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FILTERING CATEGORIES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CategorySwitchRow(
                            title = "Adult & Explicit Content",
                            subtitle = "Pornography, sexually explicit media, and escort services",
                            checked = settings.blockAdult,
                            onCheckedChange = { onUpdateSettings(settings.copy(blockAdult = it)) }
                        )

                        CategorySwitchRow(
                            title = "Gambling & Sports Betting",
                            subtitle = "Casinos, poker platforms, sports wagering, lotteries",
                            checked = settings.blockGambling,
                            onCheckedChange = { onUpdateSettings(settings.copy(blockGambling = it)) }
                        )

                        CategorySwitchRow(
                            title = "Drugs & Narcotics",
                            subtitle = "Illicit drug shops, unregulated vape & alcohol vendors",
                            checked = settings.blockDrugsAlcohol,
                            onCheckedChange = { onUpdateSettings(settings.copy(blockDrugsAlcohol = it)) }
                        )

                        CategorySwitchRow(
                            title = "Extreme Violence & Hate Speech",
                            subtitle = "Gore, terrorism propaganda, violent extremism",
                            checked = settings.blockViolenceHate,
                            onCheckedChange = { onUpdateSettings(settings.copy(blockViolenceHate = it)) }
                        )
                    }
                }
            }

            // Custom User Allow / Block rules
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CUSTOM DOMAIN RULES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newRuleDomain,
                            onValueChange = { newRuleDomain = it },
                            placeholder = { Text("e.g. example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val domain = newRuleDomain.trim().lowercase()
                                    if (domain.isNotEmpty()) {
                                        kahafGuardEngine.addUserBlock(domain)
                                        newRuleDomain = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThreatRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Always Block")
                            }

                            Button(
                                onClick = {
                                    val domain = newRuleDomain.trim().lowercase()
                                    if (domain.isNotEmpty()) {
                                        kahafGuardEngine.addUserAllow(domain)
                                        newRuleDomain = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Always Allow")
                            }
                        }
                    }
                }
            }

            // Theological & Ethical Disclaimer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GoldAccent.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Important Clarification",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RM Browser's Islamic Safe Mode is an automated family-safety tool and content classification heuristic. It does not issue absolute theological fatwas or ecclesiastical rulings. Users are advised to exercise personal discernment and mindfulness.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
