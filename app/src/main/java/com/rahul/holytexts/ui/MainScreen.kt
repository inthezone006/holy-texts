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
import com.rahul.holytexts.data.ThemePreferences
import com.rahul.holytexts.ui.theme.HolyTextsTheme
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
    object Settings : AccountNav("settings")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val scope = rememberCoroutineScope()
    
    val savedDarkMode by themePreferences.isDarkMode.collectAsState(initial = null)
    val systemDarkMode = isSystemInDarkTheme()
    val isDarkMode = savedDarkMode ?: systemDarkMode
    
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
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentDestination?.hierarchy?.any { it.route?.startsWith(screen.route) == true } == true)
                                        screen.selectedIcon
                                    else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route?.startsWith(screen.route) == true } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Library.route) { LibraryScreen() }
                
                composable(Screen.AccountTab.route) {
                    AccountScreen(
                        isDarkMode = isDarkMode,
                        onThemeChange = { newValue ->
                            scope.launch {
                                themePreferences.saveTheme(newValue)
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
                
                composable(AccountNav.Settings.route) {
                    SettingsScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
