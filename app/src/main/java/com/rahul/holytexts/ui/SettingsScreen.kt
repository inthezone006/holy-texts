package com.rahul.holytexts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahul.holytexts.BuildConfig
import com.rahul.holytexts.R
import com.rahul.holytexts.data.AppPreferences
import com.rahul.holytexts.data.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val appSettings by appPreferences.appSettings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showVerseVersionDialog by remember { mutableStateOf(false) }
    var showVerseBookDialog by remember { mutableStateOf(false) }

    val languages = mapOf("en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch")
    val versions = listOf("KJV", "ASV", "AKJV")
    val books = listOf("Psalms", "Proverbs", "Matthew", "John", "Romans")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel_btn))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Notifications
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.notifications), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(if (appSettings.notificationsEnabled) stringResource(R.string.daily_verses_enabled) else stringResource(R.string.daily_verses_disabled))
                            }
                            Switch(checked = appSettings.notificationsEnabled, onCheckedChange = { 
                                scope.launch { appPreferences.saveAppSettings(appSettings.copy(notificationsEnabled = it)) }
                            })
                        }
                    }
                }

                // Language
                item {
                    SettingsBentoItem(
                        title = stringResource(R.string.app_language),
                        subtitle = languages[appSettings.appLanguage] ?: "English",
                        icon = Icons.Default.Language,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier.height(160.dp)
                    )
                }

                // Daily Verse Version
                item {
                    SettingsBentoItem(
                        title = stringResource(R.string.verse_version),
                        subtitle = appSettings.dailyVerseVersion,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showVerseVersionDialog = true },
                        modifier = Modifier.height(160.dp)
                    )
                }

                // Daily Verse Book
                item(span = { GridItemSpan(2) }) {
                    SettingsBentoItem(
                        title = stringResource(R.string.verse_source),
                        subtitle = stringResource(R.string.prefer_verses_from, appSettings.dailyVerseBook),
                        icon = Icons.Default.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showVerseBookDialog = true },
                        modifier = Modifier.height(100.dp)
                    )
                }

                // Help & About
                item {
                    SettingsBentoItem(
                        title = stringResource(R.string.help),
                        subtitle = stringResource(R.string.support),
                        icon = Icons.AutoMirrored.Filled.HelpCenter,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(120.dp)
                    )
                }
                item {
                    SettingsBentoItem(
                        title = stringResource(R.string.about),
                        subtitle = "v${BuildConfig.VERSION_NAME}",
                        icon = Icons.Default.Info,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(120.dp)
                    )
                }
            }
        }
    }

    // Language ModalBottomSheet
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                languages.forEach { (code, name) ->
                    val isSelected = appSettings.appLanguage == code
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null // Handled by ListItem click
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { appPreferences.saveAppSettings(appSettings.copy(appLanguage = code)) }
                                    showLanguageSheet = false
                                },
                                role = Role.RadioButton
                            )
                    )
                }
            }
        }
    }

    // Standard Dialogs for other choices
    if (showVerseVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVerseVersionDialog = false },
            title = { Text(stringResource(R.string.verse_version)) },
            text = {
                Column {
                    versions.forEach { v ->
                        TextButton(onClick = {
                            scope.launch { appPreferences.saveAppSettings(appSettings.copy(dailyVerseVersion = v)) }
                            showVerseVersionDialog = false
                        }) { Text(v) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showVerseBookDialog) {
        AlertDialog(
            onDismissRequest = { showVerseBookDialog = false },
            title = { Text(stringResource(R.string.verse_source)) },
            text = {
                Column {
                    books.forEach { b ->
                        TextButton(onClick = {
                            scope.launch { appPreferences.saveAppSettings(appSettings.copy(dailyVerseBook = b)) }
                            showVerseBookDialog = false
                        }) { Text(b) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SettingsBentoItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp).align(Alignment.BottomEnd))
        }
    }
}
