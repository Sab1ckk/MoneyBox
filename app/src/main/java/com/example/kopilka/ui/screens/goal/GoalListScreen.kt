package com.example.kopilka.ui.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.ui.theme.CategoryPalette
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorAccentDim
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorBorderAccent
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.ui.theme.ColorWarning
import com.example.kopilka.utils.formatFullAmount
import com.example.kopilka.viewmodel.GoalListItemUi
import com.example.kopilka.viewmodel.GoalListUiState
import com.example.kopilka.viewmodel.GoalListViewModel
import com.example.kopilka.viewmodel.SortOption
import com.example.kopilka.viewmodel.StatusFilter

@Composable
fun GoalListScreen(
    onGoalClick: (Int) -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: GoalListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    GoalListContent(
        state = state,
        onGoalClick = onGoalClick,
        onCreateGoal = onCreateGoal,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onSortOptionChange = viewModel::onSortOptionChange,
        onToggleSortMenu = viewModel::toggleSortMenu
    )
}

@Composable
private fun GoalListContent(
    state: GoalListUiState,
    onGoalClick: (Int) -> Unit,
    onCreateGoal: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleSortMenu: () -> Unit
) {
    Scaffold(
        containerColor = ColorBase,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGoal,
                containerColor = ColorAccent,
                contentColor = ColorBase,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Создать цель")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorBase)
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Заголовок
            Column {
                Text("Мои цели", color = ColorText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "${formatFullAmount(state.totalSaved)} накоплено всего",
                    color = ColorText3,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Поиск
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Поиск целей...", color = ColorText3, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ColorText3) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ColorSurface,
                    unfocusedContainerColor = ColorSurface,
                    focusedBorderColor = ColorBorderAccent,
                    unfocusedBorderColor = ColorBorder,
                    focusedTextColor = ColorText,
                    unfocusedTextColor = ColorText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Кнопка сортировки + панель
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Сортировка: ${state.sortOption.label}",
                    color = ColorAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onToggleSortMenu() }
                        .background(ColorAccentDim, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (state.isSortMenuOpen) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(ColorCard, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    SortOption.entries.forEach { option ->
                        val selected = option == state.sortOption
                        Text(
                            text = option.label,
                            color = if (selected) ColorAccent else ColorText3,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSortOptionChange(option) }
                                .background(
                                    if (selected) ColorAccentDim else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Фильтры статуса
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusFilter.entries.forEach { filter ->
                    val selected = filter == state.statusFilter
                    Text(
                        text = filter.label,
                        color = if (selected) ColorBase else ColorText3,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onStatusFilterChange(filter) }
                            .background(
                                if (selected) ColorAccent else Color.Transparent,
                                RoundedCornerShape(100.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) ColorAccent else ColorBorder,
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                state.isLoading -> Unit
                state.items.isEmpty() -> EmptyState(onCreate = onCreateGoal)
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        GoalCard(item = item, onClick = { onGoalClick(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(item: GoalListItemUi, onClick: () -> Unit) {
    val categoryColor = CategoryPalette.colorFor(item.categoryColorIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (item.isCompleted) ColorBorderAccent else ColorBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, categoryColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.categoryIcon ?: "●", fontSize = 16.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = ColorText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        color = ColorText3,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.hasBankDeposit && item.interestRate != null) {
                    Text(
                        text = "${item.interestRate}%",
                        color = ColorAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(ColorAccentDim, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (item.isCompleted) {
                    Text(
                        text = "Готово",
                        color = ColorAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(ColorAccentDim, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Прогресс-бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(ColorSurface, RoundedCornerShape(100.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.progressPercent / 100f)
                    .height(7.dp)
                    .background(
                        if (item.isCompleted) ColorAccent else categoryColor,
                        RoundedCornerShape(100.dp)
                    )
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = formatFullAmount(item.savedAmount),
                    color = if (item.isCompleted) ColorAccent else ColorText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "из ${formatFullAmount(item.targetAmount)}",
                    color = ColorText3,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.progressPercent}%",
                    color = if (item.isCompleted || item.progressPercent >= 75) ColorAccent else ColorText3,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                val daysLabel = when {
                    item.isCompleted -> "Завершено!"
                    item.daysLeft < 0 -> "Просрочено"
                    else -> "${item.daysLeft} дн. осталось"
                }
                Text(
                    text = daysLabel,
                    color = if (item.daysLeft in 0..29 && !item.isCompleted) ColorWarning else ColorText3,
                    fontSize = 11.sp,
                    fontWeight = if (item.daysLeft in 0..29 && !item.isCompleted) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(ColorAccentDim, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("＋", color = ColorAccent, fontSize = 36.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("Пока нет целей", color = ColorText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Создайте первую цель накопления и начните отслеживать прогресс",
            color = ColorText3,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Создать цель",
            color = ColorBase,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onCreate() }
                .background(ColorAccent, RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp)
        )
    }
}