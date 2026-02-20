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
    val route: String
)

data class ReaderSettings(
    val fontSize: Float = 18f,
    val fontFamily: String = "Serif",
    val lineSpacing: Float = 1.5f,
    val readerTheme: String = "System" // "Light", "Dark", "Sepia", "System"
)

class AppPreferences(private val context: Context) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LAST_READ_BOOK = stringPreferencesKey("last_read_book")
        private val LAST_READ_CHAPTER = stringPreferencesKey("last_read_chapter")
        private val LAST_READ_ROUTE = stringPreferencesKey("last_read_route")
        
        private val FONT_SIZE = floatPreferencesKey("font_size")
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val READER_THEME = stringPreferencesKey("reader_theme")
    }

    val isDarkMode: Flow<Boolean?> = context.appDataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] }

    val lastReadInfo: Flow<LastReadInfo?> = context.appDataStore.data
        .map { preferences ->
            val book = preferences[LAST_READ_BOOK]
            val chapter = preferences[LAST_READ_CHAPTER]
            val route = preferences[LAST_READ_ROUTE]
            if (book != null && chapter != null && route != null) {
                LastReadInfo(book, chapter, route)
            } else null
        }

    val readerSettings: Flow<ReaderSettings> = context.appDataStore.data
        .map { preferences ->
            ReaderSettings(
                fontSize = preferences[FONT_SIZE] ?: 18f,
                fontFamily = preferences[FONT_FAMILY] ?: "Serif",
                lineSpacing = preferences[LINE_SPACING] ?: 1.5f,
                readerTheme = preferences[READER_THEME] ?: "System"
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
        }
    }

    suspend fun saveReaderSettings(settings: ReaderSettings) {
        context.appDataStore.edit { preferences ->
            preferences[FONT_SIZE] = settings.fontSize
            preferences[FONT_FAMILY] = settings.fontFamily
            preferences[LINE_SPACING] = settings.lineSpacing
            preferences[READER_THEME] = settings.readerTheme
        }
    }
}
