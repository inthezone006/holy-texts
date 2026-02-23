package com.rahul.holytexts.ui

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rahul.holytexts.data.AppPreferences
import com.rahul.holytexts.data.LastReadInfo
import com.rahul.holytexts.data.ReaderSettings
import com.rahul.holytexts.ui.theme.HolyTextsTheme
import com.rahul.holytexts.util.LocaleHelper
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks)
    object AccountTab : Screen("account_tab", "Account", Icons.Filled.Person, Icons.Outlined.Person)
}

sealed class AccountNav(val route: String) {
    object SignIn : AccountNav("sign_in")
    object SignUp : AccountNav("sign_up")
    object PasswordCreation : AccountNav("password_creation/{email}/{fullName}/{idToken}") {
        fun createRoute(email: String, fullName: String, idToken: String?) = 
            "password_creation/${Uri.encode(email)}/${Uri.encode(fullName)}/${Uri.encode(idToken ?: "none")}"
    }
    object ProfileSettings : AccountNav("profile_settings")
    object ChangePassword : AccountNav("change_password")
    object Bookmarks : AccountNav("bookmarks")
    object Settings : AccountNav("settings")
}

sealed class BibleNav(val route: String) {
    object BibleView : BibleNav("bible_view?book={book}&chapter={chapter}&version={version}&verse={verse}") {
        fun createRoute(book: String?, chapter: String?, version: String? = "KJV", verse: Int? = null) = 
            "bible_view?book=${Uri.encode(book ?: "Genesis")}&chapter=${Uri.encode(chapter ?: "1")}&version=${Uri.encode(version ?: "KJV")}&verse=${verse ?: -1}"
    }
}

sealed class FeatureNav(val route: String) {
    object DailyVerse : FeatureNav("daily_verse")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    
    val savedDarkMode by appPreferences.isDarkMode.collectAsState(initial = null)
    val lastReadInfo by appPreferences.lastReadInfo.collectAsState(initial = null)
    val readerSettings by appPreferences.readerSettings.collectAsState(initial = ReaderSettings())
    val appSettings by appPreferences.appSettings.collectAsState(initial = null)
    
    val systemDarkMode = isSystemInDarkTheme()
    val isDarkMode = savedDarkMode ?: systemDarkMode
    
    // Apply locale globally
    LaunchedEffect(appSettings?.appLanguage) {
        appSettings?.appLanguage?.let {
            LocaleHelper.applyLocale(context, it)
        }
    }
    
    val items = listOf(
        Screen.Home,
        Screen.Library,
        Screen.AccountTab
    )

    HolyTextsTheme(darkTheme = isDarkMode) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { 
                            it.route == screen.route || 
                            (screen == Screen.Home && it.route?.startsWith("bible_view") == true) ||
                            (screen == Screen.Home && it.route == FeatureNav.DailyVerse.route)
                        } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            onClick = {
                                if (screen == Screen.Home && (currentDestination?.route?.startsWith("bible_view") == true || currentDestination?.route == FeatureNav.DailyVerse.route)) {
                                    navController.popBackStack(Screen.Home.route, false)
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
                composable(Screen.Home.route) { 
                    HomeScreen(
                        lastReadInfo = lastReadInfo,
                        onContinueReading = { route -> 
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToBookmarks = {
                            navController.navigate(AccountNav.Bookmarks.route)
                        },
                        onNavigateToDailyVerse = {
                            navController.navigate(FeatureNav.DailyVerse.route)
                        }
                    ) 
                }
                
                composable(Screen.Library.route) { 
                    LibraryScreen(
                        onNavigateToBible = { 
                            navController.navigate(BibleNav.BibleView.createRoute("Genesis", "1", readerSettings.bibleVersion)) 
                        }
                    ) 
                }

                composable(
                    route = BibleNav.BibleView.route,
                    arguments = listOf(
                        navArgument("book") { defaultValue = "Genesis" },
                        navArgument("chapter") { defaultValue = "1" },
                        navArgument("version") { defaultValue = "KJV" },
                        navArgument("verse") { type = NavType.IntType; defaultValue = -1 }
                    )
                ) {
                    BibleScreen(
                        onBackClick = { navController.popBackStack() },
                        onUpdateLastRead = { book, chapter, route, version ->
                            scope.launch {
                                appPreferences.saveLastRead(LastReadInfo(book, chapter, route, version))
                            }
                        }
                    )
                }

                composable(FeatureNav.DailyVerse.route) {
                    DailyVerseScreen(onBackClick = { navController.popBackStack() })
                }
                
                composable(Screen.AccountTab.route) {
                    AccountScreen(
                        isDarkMode = isDarkMode,
                        onThemeChange = { newValue ->
                            scope.launch {
                                appPreferences.saveTheme(newValue)
                            }
                        },
                        onNavigateToSignIn = { navController.navigate(AccountNav.SignIn.route) },
                        onNavigateToSignUp = { navController.navigate(AccountNav.SignUp.route) },
                        onNavigateToProfileSettings = { navController.navigate(AccountNav.ProfileSettings.route) },
                        onNavigateToSettings = { navController.navigate(AccountNav.Settings.route) }
                    )
                }
                
                composable(AccountNav.SignIn.route) { 
                    SignInScreen(
                        isDark = isDarkMode,
                        onBackClick = { navController.popBackStack() },
                        onSignInSuccess = { 
                            navController.popBackStack(Screen.AccountTab.route, false)
                        }
                    ) 
                }
                
                composable(AccountNav.SignUp.route) { 
                    SignUpScreen(
                        isDark = isDarkMode,
                        onBackClick = { navController.popBackStack() },
                        onContinueToPassword = { email, name, token ->
                            navController.navigate(AccountNav.PasswordCreation.createRoute(email, name, token))
                        }
                    ) 
                }

                composable(
                    route = AccountNav.PasswordCreation.route,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("fullName") { type = NavType.StringType },
                        navArgument("idToken") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = Uri.decode(backStackEntry.arguments?.getString("email") ?: "")
                    val fullName = Uri.decode(backStackEntry.arguments?.getString("fullName") ?: "")
                    val idTokenRaw = Uri.decode(backStackEntry.arguments?.getString("idToken") ?: "none")
                    val idToken = if (idTokenRaw == "none") null else idTokenRaw
                    
                    PasswordCreationScreen(
                        email = email,
                        fullName = fullName,
                        googleIdToken = idToken,
                        onBackClick = { navController.popBackStack() },
                        onComplete = {
                            navController.navigate(Screen.AccountTab.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    )
                }

                composable(AccountNav.ProfileSettings.route) {
                    ProfileSettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onChangePasswordClick = { navController.navigate(AccountNav.ChangePassword.route) },
                        onDeleteAccountSuccess = {
                            navController.navigate(Screen.AccountTab.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    )
                }

                composable(AccountNav.ChangePassword.route) {
                    ChangePasswordScreen(
                        onBackClick = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                }

                composable(AccountNav.Bookmarks.route) {
                    BookmarksScreen(
                        onBackClick = { navController.popBackStack() },
                        onBookmarkClick = { book, chapter, version, verse ->
                            navController.navigate(BibleNav.BibleView.createRoute(book, chapter.toString(), version, verse))
                        }
                    )
                }
                
                composable(AccountNav.Settings.route) {
                    SettingsScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
