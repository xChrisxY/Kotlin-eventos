package com.example.events

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.events.data.api.AuthService
import com.example.events.ui.screens.HomeScreen
import com.example.events.ui.screens.LoginScreen

@Composable
fun EventsApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authService = AuthService()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(navController, authService)
        }
        composable("home") {
            HomeScreen()
        }
    }
}