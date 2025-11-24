package com.kotla.anifloat.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kotla.anifloat.ui.screens.HomeScreen
import com.kotla.anifloat.ui.screens.LoginScreen
import com.kotla.anifloat.ui.screens.LoginViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = viewModel(factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application))
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // Determine start destination based on initial state (simple check)
    // Note: This simple check might flicker if the token takes time to load.
    // Ideally, we have a "Splash" or "Loading" state.
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
    }
}
