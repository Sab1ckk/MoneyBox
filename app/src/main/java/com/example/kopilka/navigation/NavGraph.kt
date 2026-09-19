package com.example.kopilka.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kopilka.ui.components.BottomNav
import com.example.kopilka.ui.screens.goal.GoalListScreen
import com.example.kopilka.ui.screens.pin.PinScreen

object Routes {
    const val PIN = "pin"
    const val GOALS = "goals"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
}

private val BOTTOM_NAV_ROUTES = setOf(Routes.GOALS, Routes.STATISTICS, Routes.SETTINGS)

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNav(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        if (tab.route != currentRoute) {
                            navController.navigate(tab.route) {
                                // Стандартный паттерн переключения вкладок: не плодим
                                // бэкстек, сохраняем/восстанавливаем состояние каждой вкладки.
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.PIN) {
                PinScreen(
                    onUnlocked = {
                        navController.navigate(Routes.GOALS) {
                            popUpTo(Routes.PIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.GOALS) {
                GoalListScreen(
                    onGoalClick = { goalId ->
                        // TODO: навигация на GoalDetailsScreen, когда экран будет готов
                    },
                    onCreateGoal = {
                        // TODO: навигация на CreateGoalScreen, когда экран будет готов
                    }
                )
            }

            composable(Routes.STATISTICS) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Статистика — TODO")
                }
            }

            composable(Routes.SETTINGS) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Настройки — TODO")
                }
            }
        }
    }
}