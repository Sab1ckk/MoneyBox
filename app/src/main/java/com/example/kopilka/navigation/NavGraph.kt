// Путь в проекте: app/src/main/java/com/example/kopilka/navigation/NavGraph.kt

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kopilka.ui.components.BottomNav
import com.example.kopilka.ui.screens.goals.CreateGoalScreen
import com.example.kopilka.ui.screens.goals.GoalDetailsScreen
import com.example.kopilka.ui.screens.goals.GoalListScreen
import com.example.kopilka.ui.screens.pin.PinScreen
import com.example.kopilka.ui.screens.settings.SettingsScreen
import com.example.kopilka.ui.screens.statistics.StatisticsScreen

object Routes {
    const val PIN = "pin"
    const val GOALS = "goals"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val CREATE_GOAL = "createGoal?goalId={goalId}"
    const val GOAL_DETAILS = "goalDetails/{goalId}"

    fun createGoal(goalId: Int? = null) = "createGoal?goalId=${goalId ?: -1}"
    fun goalDetails(goalId: Int) = "goalDetails/$goalId"
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
                        navController.navigate(Routes.goalDetails(goalId))
                    },
                    onCreateGoal = {
                        navController.navigate(Routes.createGoal())
                    }
                )
            }

            composable(
                route = Routes.CREATE_GOAL,
                arguments = listOf(navArgument("goalId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) {
                CreateGoalScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.GOAL_DETAILS,
                arguments = listOf(navArgument("goalId") { type = NavType.IntType })
            ) {
                GoalDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { goalId -> navController.navigate(Routes.createGoal(goalId)) }
                )
            }

            composable(Routes.STATISTICS) {
                StatisticsScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onChangePin = {
                        navController.navigate(Routes.PIN)
                    }
                )

            }
        }
    }
}
