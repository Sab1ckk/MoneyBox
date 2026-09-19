package com.example.kopilka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kopilka.navigation.Routes
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorText3

enum class BottomNavTab(val route: String, val label: String, val emoji: ImageVector) {
    GOALS(Routes.GOALS, "Цели", Icons.Filled.Home),
    STATISTICS(Routes.STATISTICS, "Статистика", Icons.Filled.Info),
    SETTINGS(Routes.SETTINGS, "Настройки", Icons.Filled.Settings)
}

@Composable
fun BottomNav(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = tab.route == currentRoute
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = tab.emoji,
                    contentDescription = tab.label,
                    tint = if (selected) ColorAccent else ColorText3
                )
                Text(
                    text = tab.label,
                    fontSize = 11.sp,
                    color = if (selected) ColorAccent else ColorText3,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
