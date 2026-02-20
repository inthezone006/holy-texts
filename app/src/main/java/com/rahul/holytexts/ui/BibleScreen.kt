package com.rahul.holytexts.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahul.holytexts.data.AppPreferences
import com.rahul.holytexts.data.ReaderSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BibleScreen(
    onBackClick: () -> Unit,
    onUpdateLastRead: (String, String, String) -> Unit,
    viewModel: BibleViewModel = viewModel()
) {
    val verses by viewModel.verses.collectAsState()
    val books by viewModel.books.collectAsState()
    val currentBook by viewModel.currentBook.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navDirection by viewModel.navDirection.collectAsState()
    val highlights by viewModel.highlights.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val scrollTargetVerse by viewModel.scrollTargetVerse.collectAsState()

    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val readerSettings by appPreferences.readerSettings.collectAsState(initial = ReaderSettings())
    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    
    var isSearchVisible by remember { mutableStateOf(true) }
    var expandedBook by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedVerseForMenu by remember { mutableStateOf<Verse?>(null) }

    // Handle Auto-scroll to target verse
    LaunchedEffect(verses, scrollTargetVerse) {
        if (verses.isNotEmpty() && scrollTargetVerse != null) {
            val index = verses.indexOfFirst { it.verse == scrollTargetVerse }
            if (index != -1) {
                delay(300) // Wait for layout/animation
                listState.animateScrollToItem(index)
                viewModel.clearScrollTarget()
            }
        }
    }

    // Theme Colors
    val backgroundColor = when (readerSettings.readerTheme) {
        "Sepia" -> Color(0xFFF4ECD8)
        "Dark" -> Color(0xFF121212)
        "Light" -> Color.White
        else -> MaterialTheme.colorScheme.background
    }
    val contentColor = when (readerSettings.readerTheme) {
        "Sepia" -> Color(0xFF5B4636)
        "Dark" -> Color.White
        "Light" -> Color.Black
        else -> MaterialTheme.colorScheme.onBackground
    }

    val selectedFontFamily = when (readerSettings.fontFamily) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isSearchVisible = false
        }
    }

    LaunchedEffect(currentBook, currentChapter) {
        onUpdateLastRead(currentBook, "Chapter $currentChapter", "bible_view")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Books", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(books) { book ->
                        val isExpanded = expandedBook == book.name
                        Column {
                            NavigationDrawerItem(
                                label = { Text(book.name) },
                                selected = currentBook == book.name && !isExpanded,
                                onClick = { expandedBook = if (isExpanded) null else book.name },
                                badge = { Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
                            )
                            if (isExpanded) {
                                for (i in 1..book.chapters) {
                                    val isSelected = currentBook == book.name && currentChapter == i
                                    NavigationDrawerItem(
                                        label = { Text("Chapter $i") },
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.loadChapter(book.name, i, 0)
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$currentBook $currentChapter") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, "Reader Settings")
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            },
            floatingActionButton = {
                AnimatedVisibility(visible = isSearchVisible, enter = fadeIn(), exit = fadeOut()) {
                    FloatingActionButton(
                        onClick = { showSearchSheet = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) { Icon(Icons.Default.Search, "Search") }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(backgroundColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 50) viewModel.previousChapter()
                            if (dragAmount < -50) viewModel.nextChapter()
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isSearchVisible = true }
            ) {
                // Page Flip / Slide Animation
                AnimatedContent(
                    targetState = "$currentBook-$currentChapter",
                    transitionSpec = {
                        if (navDirection > 0) {
                            (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn())
                                .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut())
                        } else if (navDirection < 0) {
                            (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn())
                                .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut())
                        } else {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        }
                    }
                ) { targetState ->
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(verses) { verse ->
                                val isHighlighted = highlights.contains(verse.verse)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp)) // Curve the corners
                                        .background(if (isHighlighted) Color.Yellow.copy(alpha = 0.3f) else Color.Transparent)
                                        .combinedClickable(
                                            onClick = { selectedVerseForMenu = verse },
                                            onLongClick = { selectedVerseForMenu = verse }
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = verse.verse.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = verse.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = readerSettings.fontSize.sp,
                                            fontFamily = selectedFontFamily,
                                            lineHeight = (readerSettings.fontSize * readerSettings.lineSpacing).sp,
                                            color = contentColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Verse Context Menu - ModalBottomSheet
    if (selectedVerseForMenu != null) {
        val verse = selectedVerseForMenu!!
        ModalBottomSheet(
            onDismissRequest = { selectedVerseForMenu = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "${verse.book} ${verse.chapter}:${verse.verse}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(if (highlights.contains(verse.verse)) "Remove Highlight" else "Highlight Verse", fontWeight = FontWeight.SemiBold) },
                            leadingContent = { 
                                Icon(
                                    imageVector = if (highlights.contains(verse.verse)) Icons.Default.HighlightOff else Icons.Default.Highlight,
                                    contentDescription = null,
                                    tint = if (highlights.contains(verse.verse)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ) 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.toggleHighlight(verse.verse)
                                selectedVerseForMenu = null
                            }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        
                        ListItem(
                            headlineContent = { Text("Share Verse", fontWeight = FontWeight.SemiBold) },
                            leadingContent = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${verse.book} ${verse.chapter}:${verse.verse} - ${verse.text}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                                selectedVerseForMenu = null
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom Search UI
    if (showSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search text, verse, or book...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true
                )
                
                Spacer(Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(searchResults) { verse ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.loadChapter(verse.book, verse.chapter, 0, verse.verse)
                                    showSearchSheet = false
                                    searchQuery = ""
                                    viewModel.search("")
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${verse.book} ${verse.chapter}:${verse.verse}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = verse.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Reader Settings", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                
                Text("Font Size: ${readerSettings.fontSize.toInt()}")
                Slider(
                    value = readerSettings.fontSize,
                    onValueChange = { scope.launch { appPreferences.saveReaderSettings(readerSettings.copy(fontSize = it)) } },
                    valueRange = 12f..32f
                )

                Text("Line Spacing: ${"%.1f".format(readerSettings.lineSpacing)}")
                Slider(
                    value = readerSettings.lineSpacing,
                    onValueChange = { scope.launch { appPreferences.saveReaderSettings(readerSettings.copy(lineSpacing = it)) } },
                    valueRange = 1.0f..2.5f
                )

                Text("Font Family")
                Row {
                    listOf("Default", "Serif", "SansSerif", "Monospace").forEach { font ->
                        FilterChip(
                            selected = readerSettings.fontFamily == font,
                            onClick = { scope.launch { appPreferences.saveReaderSettings(readerSettings.copy(fontFamily = font)) } },
                            label = { Text(font) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Text("Theme")
                Row {
                    listOf("System", "Light", "Dark", "Sepia").forEach { theme ->
                        FilterChip(
                            selected = readerSettings.readerTheme == theme,
                            onClick = { scope.launch { appPreferences.saveReaderSettings(readerSettings.copy(readerTheme = theme)) } },
                            label = { Text(theme) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
