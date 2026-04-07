package com.mountaincrab.crabdo.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.theme.AppTheme
import com.mountaincrab.crabdo.ui.theme.GradientIconBlock
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val pinnedBoardId by viewModel.pinnedBoardId.collectAsStateWithLifecycle()
    val currentTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientIconBlock(icon = Icons.Default.Settings, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Account card
            SectionCard {
                ListItem(
                    headlineContent = {
                        Text(
                            if (viewModel.isAnonymous) "Anonymous user" else viewModel.userEmail ?: "",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(
                            if (viewModel.isAnonymous) "Link a Google account to back up your data"
                            else "Signed in with Google"
                        )
                    },
                    trailingContent = {
                        if (viewModel.isAnonymous) {
                            TextButton(onClick = { /* TODO: trigger Google Sign-In */ }) {
                                Text("Link")
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            // Appearance
            SectionLabel("APPEARANCE")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppTheme.entries.forEach { theme ->
                            ThemeSwatch(
                                theme = theme,
                                selected = theme == currentTheme,
                                onClick = { themeViewModel.setTheme(theme) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pinned board
            SectionLabel("PINNED BOARD")
            SectionCard {
                var expanded by remember { mutableStateOf(false) }
                val pinnedBoard = boards.find { it.id == pinnedBoardId }
                ListItem(
                    headlineContent = { Text("Pinned board", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(pinnedBoard?.title ?: "None") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expanded = true }) { Text("Change") }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = { viewModel.setPinnedBoard(null); expanded = false }
                                )
                                boards.forEach { board ->
                                    DropdownMenuItem(
                                        text = { Text(board.title) },
                                        onClick = { viewModel.setPinnedBoard(board.id); expanded = false }
                                    )
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            // Exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.canScheduleExactAlarms) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Exact alarms not permitted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap to grant permission for precise reminder timing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // About
            SectionLabel("ABOUT")
            SectionCard {
                ListItem(
                    headlineContent = { Text("Crab Do", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Version 1.0") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        content()
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when (theme) {
        AppTheme.DEEP_NAVY -> Color(0xFF131A2E)
        AppTheme.CHARCOAL -> Color(0xFF141414)
        AppTheme.SLATE -> Color(0xFF20232E)
    }
    val inner = when (theme) {
        AppTheme.DEEP_NAVY -> Color(0xFF1C2340)
        AppTheme.CHARCOAL -> Color(0xFF1E1E1E)
        AppTheme.SLATE -> Color(0xFF2A2E3C)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .then(
                    if (selected)
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                    else Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                )
                .clickable(onClick = onClick)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(inner)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(inner)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
