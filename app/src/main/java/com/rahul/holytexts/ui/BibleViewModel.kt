package com.rahul.holytexts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class Verse(val book: String, val chapter: Int, val verse: Int, val text: String)
data class BibleBook(val name: String, val chapters: Int)

class BibleViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    val verses: StateFlow<List<Verse>> = _verses

    private val _books = MutableStateFlow<List<BibleBook>>(emptyList())
    val books: StateFlow<List<BibleBook>> = _books

    private val _currentBook = MutableStateFlow("Genesis")
    val currentBook: StateFlow<String> = _currentBook

    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _highlights = MutableStateFlow<Set<Int>>(emptySet())
    val highlights: StateFlow<Set<Int>> = _highlights

    private val _navDirection = MutableStateFlow(0)
    val navDirection: StateFlow<Int> = _navDirection

    private val _searchResults = MutableStateFlow<List<Verse>>(emptyList())
    val searchResults: StateFlow<List<Verse>> = _searchResults

    private val _scrollTargetVerse = MutableStateFlow<Int?>(null)
    val scrollTargetVerse: StateFlow<Int?> = _scrollTargetVerse

    private val _allVerses = mutableListOf<Verse>()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        viewModelScope.launch {
            loadBibleData()
            val bookArg = savedStateHandle.get<String>("book")
            val chapterArg = savedStateHandle.get<String>("chapter")?.toIntOrNull()
            
            if (bookArg != null && chapterArg != null) {
                loadChapter(bookArg, chapterArg, 0)
            } else {
                loadChapter("Genesis", 1, 0)
            }
        }
    }

    private suspend fun loadBibleData() {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().assets.open("bible.txt")
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val bookChapterMap = LinkedHashMap<String, Int>()
                
                reader.forEachLine { line ->
                    val match = Regex("""^(.+?)\s+(\d+):(\d+)\t+(.*)$""").find(line)
                    if (match != null) {
                        val bookName = match.groupValues[1].trim()
                        val chapterNum = match.groupValues[2].toInt()
                        val verseNum = match.groupValues[3].toInt()
                        val text = match.groupValues[4]
                        
                        val verse = Verse(bookName, chapterNum, verseNum, text)
                        _allVerses.add(verse)

                        val currentMax = bookChapterMap.getOrDefault(bookName, 0)
                        if (chapterNum > currentMax) {
                            bookChapterMap[bookName] = chapterNum
                        }
                    }
                }
                _books.value = bookChapterMap.map { (name, chapters) -> BibleBook(name, chapters) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadChapter(bookName: String, chapter: Int, direction: Int = 0, targetVerse: Int? = null) {
        _currentBook.value = bookName
        _currentChapter.value = chapter
        _navDirection.value = direction
        _scrollTargetVerse.value = targetVerse
        _isLoading.value = true
        
        savedStateHandle["book"] = bookName
        savedStateHandle["chapter"] = chapter.toString()

        viewModelScope.launch {
            loadHighlights(bookName, chapter)
            _verses.value = _allVerses.filter { 
                it.book.equals(bookName, ignoreCase = true) && it.chapter == chapter 
            }
            _isLoading.value = false
        }
    }

    fun clearScrollTarget() {
        _scrollTargetVerse.value = null
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = withContext(Dispatchers.Default) {
                _allVerses.filter { 
                    it.text.contains(query, ignoreCase = true) || 
                    it.book.contains(query, ignoreCase = true) ||
                    "${it.book} ${it.chapter}:${it.verse}".contains(query, ignoreCase = true)
                }.take(100)
            }
        }
    }

    private suspend fun loadHighlights(book: String, chapter: Int) {
        val user = auth.currentUser ?: return
        try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("highlights")
                .whereEqualTo("book", book)
                .whereEqualTo("chapter", chapter)
                .get()
                .await()
            
            val highlightSet = snapshot.documents.mapNotNull { it.getLong("verse")?.toInt() }.toSet()
            _highlights.value = highlightSet
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleHighlight(verse: Int) {
        val user = auth.currentUser ?: return
        val book = _currentBook.value
        val chapter = _currentChapter.value
        val isHighlighted = _highlights.value.contains(verse)

        viewModelScope.launch {
            try {
                val docId = "${book}_${chapter}_${verse}"
                if (isHighlighted) {
                    db.collection("users").document(user.uid)
                        .collection("highlights").document(docId)
                        .delete().await()
                    _highlights.value = _highlights.value - verse
                } else {
                    val highlight = hashMapOf(
                        "book" to book,
                        "chapter" to chapter,
                        "verse" to verse
                    )
                    db.collection("users").document(user.uid)
                        .collection("highlights").document(docId)
                        .set(highlight).await()
                    _highlights.value = _highlights.value + verse
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun nextChapter() {
        if (_isLoading.value) return
        val currentIdx = _books.value.indexOfFirst { it.name.equals(_currentBook.value, ignoreCase = true) }
        if (currentIdx == -1) return

        val book = _books.value[currentIdx]
        if (_currentChapter.value < book.chapters) {
            loadChapter(book.name, _currentChapter.value + 1, 1)
        } else if (currentIdx < _books.value.size - 1) {
            val nextBook = _books.value[currentIdx + 1]
            loadChapter(nextBook.name, 1, 1)
        }
    }

    fun previousChapter() {
        if (_isLoading.value) return
        val currentIdx = _books.value.indexOfFirst { it.name.equals(_currentBook.value, ignoreCase = true) }
        if (currentIdx == -1) return

        if (_currentChapter.value > 1) {
            loadChapter(_currentBook.value, _currentChapter.value - 1, -1)
        } else if (currentIdx > 0) {
            val prevBook = _books.value[currentIdx - 1]
            loadChapter(prevBook.name, prevBook.chapters, -1)
        }
    }
}
