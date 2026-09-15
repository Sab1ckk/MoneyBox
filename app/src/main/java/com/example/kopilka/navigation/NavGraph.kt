
package com.example.kopilka.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kopilka.ui.screens.pin.PinScreen

object Routes {
    const val PIN = "pin"
    const val GOALS = "goals"
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.PIN
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
            // TODO: заменить на реальный GoalListScreen, когда будем его делать
            Box(modifier = Modifier.fillMaxSize()) {
                Text("Goals screen — TODO", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
