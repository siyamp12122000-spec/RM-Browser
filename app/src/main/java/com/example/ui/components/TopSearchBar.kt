package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserTab
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ThreatRed

@Composable
fun TopSearchBar(
    tab: BrowserTab?,
    tabCount: Int,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSecurityInspector: () -> Unit,
    onOpenSecurityDashboard: () -> Unit,
    onOpenIslamicSafeMode: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    isBookmarked: Boolean,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onClearBrowsingData: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Kahaf Guard Shield / Security Indicator
            IconButton(
                onClick = onOpenSecurityInspector,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("security_shield_button")
            ) {
                val isSsl = tab?.isSslSecure != false
                val isBlocked = tab?.isBlocked == true
                val shieldColor = when {
                    isBlocked -> ThreatRed
                    isSsl -> EmeraldPrimary
                    else -> GoldAccent
                }
                Icon(
                    imageVector = if (isBlocked) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = "Kahaf Guard Security Status",
                    tint = shieldColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Search / URL pill
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    if (tab?.isSslSecure == true && tab.url.startsWith("https://")) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "HTTPS Secure Connection",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    BasicTextField(
                        value = urlInput,
                        onValueChange = onUrlInputChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(EmeraldPrimary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onNavigate(urlInput)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (urlInput.isEmpty()) {
                                Text(
                                    text = "Search or type URL (Halal & Safe)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("url_input_field")
                    )

                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = { onUrlInputChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (tab?.isLoading == true) {
                        IconButton(
                            onClick = onReload,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onReload,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tabs button with badge counter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenTabs() }
                    .testTag("tab_counter_button")
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$tabCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Menu overflow button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("menu_overflow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("🛡 Security Dashboard", fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            menuExpanded = false
                            onOpenSecurityDashboard()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🕌 Islamic Safe Mode", fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            menuExpanded = false
                            onOpenIslamicSafeMode()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Mosque, contentDescription = null, tint = GoldAccent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        onClick = {
                            menuExpanded = false
                            onNewTab()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Incognito Tab") },
                        onClick = {
                            menuExpanded = false
                            onNewIncognitoTab()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Shield, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Page") },
                        onClick = {
                            menuExpanded = false
                            onToggleBookmark()
                        },
                        leadingIcon = {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) GoldAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        onClick = {
                            menuExpanded = false
                            onOpenBookmarks()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Browsing History") },
                        onClick = {
                            menuExpanded = false
                            onOpenHistory()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        onClick = {
                            menuExpanded = false
                            onOpenDownloads()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDesktopMode) "✓ Desktop Site" else "Desktop Site") },
                        onClick = {
                            menuExpanded = false
                            onToggleDesktopMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Browsing Data") },
                        onClick = {
                            menuExpanded = false
                            onClearBrowsingData()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ThreatRed)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }
            }
        }

        // Web Page Loading Progress indicator
        AnimatedVisibility(visible = tab?.isLoading == true && (tab.progress in 1..99)) {
            LinearProgressIndicator(
                progress = { (tab?.progress ?: 0) / 100f },
                color = EmeraldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}
