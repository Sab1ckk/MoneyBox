package com.example.kopilka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kopilka.ui.screens.goal.GoalListScreen

@Composable
fun NavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "goals"
    ) {
        composable("goals") {
            GoalListScreen()
        }
    }
}