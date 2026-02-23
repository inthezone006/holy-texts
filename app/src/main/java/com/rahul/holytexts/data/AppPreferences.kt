package com.rahul.holytexts.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

data class LastReadInfo(
    val bookName: String,
    val chapterName: String,
    val route: String,
    val version: String = "KJV"
)

data class ReaderSettings(
    val fontSize: Float = 18f,
    val fontFamily: String = "Serif",
    val lineSpacing: Float = 1.5f,
    val readerTheme: String = "System", // "Light", "Dark", "Sepia", "System"
    val bibleVersion: String = "KJV" // "KJV", "ASV", "AKJV"
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val appLanguage: String = "en", // Store as locale code: "en", "es", etc.
    val dailyVerseVersion: String = "KJV",
    val dailyVerseBook: String = "Psalms"
)

class AppPreferences(private val context: Context) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LAST_READ_BOOK = stringPreferencesKey("last_read_book")
        private val LAST_READ_CHAPTER = stringPreferencesKey("last_read_chapter")
        private val LAST_READ_ROUTE = stringPreferencesKey("last_read_route")
        private val LAST_READ_VERSION = stringPreferencesKey("last_read_version")
        
        private val FONT_SIZE = floatPreferencesKey("font_size")
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val READER_THEME = stringPreferencesKey("reader_theme")
        private val BIBLE_VERSION = stringPreferencesKey("bible_version")

        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
        private val DAILY_VERSE_VERSION = stringPreferencesKey("daily_verse_version")
        private val DAILY_VERSE_BOOK = stringPreferencesKey("daily_verse_book")
    }

    val isDarkMode: Flow<Boolean?> = context.appDataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] }

    val lastReadInfo: Flow<LastReadInfo?> = context.appDataStore.data
        .map { preferences ->
            val book = preferences[LAST_READ_BOOK]
            val chapter = preferences[LAST_READ_CHAPTER]
            val route = preferences[LAST_READ_ROUTE]
            val version = preferences[LAST_READ_VERSION] ?: "KJV"
            if (book != null && chapter != null && route != null) {
                LastReadInfo(book, chapter, route, version)
            } else null
        }

    val readerSettings: Flow<ReaderSettings> = context.appDataStore.data
        .map { preferences ->
            ReaderSettings(
                fontSize = preferences[FONT_SIZE] ?: 18f,
                fontFamily = preferences[FONT_FAMILY] ?: "Serif",
                lineSpacing = preferences[LINE_SPACING] ?: 1.5f,
                readerTheme = preferences[READER_THEME] ?: "System",
                bibleVersion = preferences[BIBLE_VERSION] ?: "KJV"
            )
        }

    val appSettings: Flow<AppSettings> = context.appDataStore.data
        .map { preferences ->
            AppSettings(
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                appLanguage = preferences[APP_LANGUAGE] ?: "en",
                dailyVerseVersion = preferences[DAILY_VERSE_VERSION] ?: "KJV",
                dailyVerseBook = preferences[DAILY_VERSE_BOOK] ?: "Psalms"
            )
        }

    suspend fun saveTheme(isDarkMode: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    suspend fun saveLastRead(info: LastReadInfo) {
        context.appDataStore.edit { preferences ->
            preferences[LAST_READ_BOOK] = info.bookName
            preferences[LAST_READ_CHAPTER] = info.chapterName
            preferences[LAST_READ_ROUTE] = info.route
            preferences[LAST_READ_VERSION] = info.version
        }
    }

    suspend fun saveReaderSettings(settings: ReaderSettings) {
        context.appDataStore.edit { preferences ->
            preferences[FONT_SIZE] = settings.fontSize
            preferences[FONT_FAMILY] = settings.fontFamily
            preferences[LINE_SPACING] = settings.lineSpacing
            preferences[READER_THEME] = settings.readerTheme
            preferences[BIBLE_VERSION] = settings.bibleVersion
        }
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        context.appDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[APP_LANGUAGE] = settings.appLanguage
            preferences[DAILY_VERSE_VERSION] = settings.dailyVerseVersion
            preferences[DAILY_VERSE_BOOK] = settings.dailyVerseBook
        }
    }
}
