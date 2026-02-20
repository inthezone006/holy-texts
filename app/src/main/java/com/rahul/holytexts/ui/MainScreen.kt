package com.rahul.holytexts.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rahul.holytexts.ui.theme.HolyTextsTheme

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object AccountTab : Screen("account_tab", "Account", Icons.Filled.Person, Icons.Outlined.Person)
}

sealed class AccountNav(val route: String) {
    object SignIn : AccountNav("sign_in")
    object SignUp : AccountNav("sign_up")
    object Settings : AccountNav("settings")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var isDarkMode by remember { mutableStateOf(false) }
    
    val items = listOf(
        Screen.Home,
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
                
                composable(Screen.AccountTab.route) {
                    AccountScreen(
                        isDarkMode = isDarkMode,
                        onThemeChange = { isDarkMode = it },
                        onNavigateToSignIn = { navController.navigate(AccountNav.SignIn.route) },
                        onNavigateToSignUp = { navController.navigate(AccountNav.SignUp.route) },
                        onNavigateToSettings = { navController.navigate(AccountNav.Settings.route) }
                    )
                }
                
                composable(AccountNav.SignIn.route) { 
                    SignInScreen(onSignInSuccess = { navController.popBackStack() }) 
                }
                
                composable(AccountNav.SignUp.route) { 
                    SignUpScreen(onSignUpSuccess = { navController.popBackStack() }) 
                }
                
                composable(AccountNav.Settings.route) {
                    SettingsScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
