package com.example.kopilka.ui.screens.goals

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
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.kopilka.ui.theme.ColorError

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
        onToggleSortMenu = viewModel::toggleSortMenu,
        onToggleCategory = viewModel::onToggleCategory,
        onClearCategories = viewModel::onClearCategories
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
    onToggleSortMenu: () -> Unit,
    onToggleCategory: (Int) -> Unit,
    onClearCategories: () -> Unit
) {

    var filtersOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    val activeFiltersCount = (if (state.statusFilter != StatusFilter.ALL) 1 else 0) +
            state.selectedCategoryIds.size

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
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            )

            Spacer(Modifier.height(12.dp))

            // Кнопки: "Фильтры" слева, "Сортировка" справа
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val hasActive = activeFiltersCount > 0
                Text(
                    text = buildString {
                        append("Фильтры")
                        if (hasActive) append(" · $activeFiltersCount")
                        append(if (filtersOpen) "  ▴" else "  ▾")
                    },
                    color = if (hasActive || filtersOpen) ColorAccent else ColorText3,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { filtersOpen = !filtersOpen }
                        .background(
                            if (hasActive || filtersOpen) ColorAccentDim else ColorSurface,
                            RoundedCornerShape(100.dp)
                        )
                        .border(
                            1.dp,
                            if (hasActive || filtersOpen) ColorBorderAccent else ColorBorder,
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "Сортировка: ${state.sortOption.label}",
                    color = ColorAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable {
                            onToggleSortMenu()
                        }
                        .background(ColorAccentDim, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            AnimatedVisibility(visible = state.isSortMenuOpen){
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
                                    .clickable {
                                        onSortOptionChange(option)
                                    }
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

            Spacer(Modifier.height(6.dp))

            AnimatedVisibility(visible = filtersOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(ColorCard, RoundedCornerShape(16.dp))
                        .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    // Статус
                    Text("СТАТУС", color = ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusFilter.entries.forEach { filter ->
                            val selected = filter == state.statusFilter
                            Text(
                                text = filter.label,
                                color = if (selected) ColorBase else ColorText3,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
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
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // Категории
                    if (state.categories.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("КАТЕГОРИИ", color = ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val allSelected = state.selectedCategoryIds.isEmpty()
                            Text(
                                text = "Все",
                                color = if (allSelected) ColorBase else ColorText3,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable { onClearCategories() }
                                    .background(
                                        if (allSelected) ColorAccent else Color.Transparent,
                                        RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (allSelected) ColorAccent else ColorBorder,
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                            state.categories.forEach { category ->
                                val selected = category.id in state.selectedCategoryIds
                                val color = CategoryPalette.colorForCategory(category)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable { onToggleCategory(category.id) }
                                        .background(
                                            if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
                                            RoundedCornerShape(100.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) color else ColorBorder,
                                            RoundedCornerShape(100.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(category.icon.ifBlank { "●" }, fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = category.name,
                                        color = if (selected) color else ColorText3,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Сброс всех фильтров
                    if (activeFiltersCount > 0) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Сбросить фильтры",
                            color = ColorError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onStatusFilterChange(StatusFilter.ALL)
                                    onClearCategories()
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Unit
                state.items.isEmpty() && (state.searchQuery.isNotBlank() ||
                        state.statusFilter != StatusFilter.ALL ||
                        state.selectedCategoryIds.isNotEmpty()) -> Text(
                    text = "Ничего не найдено. Измените фильтры или поиск",
                    color = ColorText3,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 40.dp, start = 8.dp, end = 8.dp)
                )
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
    val categoryColor = CategoryPalette.colorForCategory(item.category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
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
                    item.daysLeft == null -> "Нет прогноза"
                    item.daysLeft < 0 -> "Просрочено"
                    item.isDeadlineEstimated -> "~${item.daysLeft} дн. (прогноз)"
                    else -> "${item.daysLeft} дн. осталось"
                }
                val isUrgent = item.daysLeft != null && item.daysLeft in 0..29 && !item.isCompleted
                Text(
                    text = daysLabel,
                    color = if (isUrgent) ColorWarning else ColorText3,
                    fontSize = 11.sp,
                    fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCreate() }
                .background(ColorAccent, RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}

//@Preview(showBackground = true, backgroundColor = 0xFF0A0F0D)
//@Composable
//fun GoalListContentPreview() {
//    val fakeItems = listOf(
//        GoalListItemUi(
//            id = 1,
//            name = "Поездка в Японию",
//            description = "Цветение сакуры — Токио, Киото",
//            categoryColorIndex = 0,
//            categoryIcon = "✈",
//            hasBankDeposit = true,
//            interestRate = 14.5,
//            isCompleted = false,
//            progressPercent = 36,
//            savedAmount = 127500.0,
//            targetAmount = 350000.0,
//            daysLeft = 120,
//            isDeadlineEstimated = false
//        ),
//        GoalListItemUi(
//            id = 2,
//            name = "MacBook Pro M4",
//            description = "Для работы и монтажа видео",
//            categoryColorIndex = 1,
//            categoryIcon = "💻",
//            hasBankDeposit = false,
//            interestRate = null,
//            isCompleted = true,
//            progressPercent = 100,
//            savedAmount = 250000.0,
//            targetAmount = 250000.0,
//            daysLeft = null,
//            isDeadlineEstimated = false
//        ),
//        GoalListItemUi(
//            id = 3,
//            name = "Подушка безопасности",
//            description = "Запас на 6 месяцев расходов",
//            categoryColorIndex = 4,
//            categoryIcon = "🛡",
//            hasBankDeposit = true,
//            interestRate = 16.0,
//            isCompleted = false,
//            progressPercent = 30,
//            savedAmount = 180000.0,
//            targetAmount = 600000.0,
//            daysLeft = 15,
//            isDeadlineEstimated = true
//        )
//    )
//
//    KopilkaTheme {
//        GoalListContent(
//            state = GoalListUiState(
//                isLoading = false,
//                searchQuery = "",
//                statusFilter = StatusFilter.ALL,
//                sortOption = SortOption.DEADLINE,
//                isSortMenuOpen = false,
//                items = fakeItems,
//                totalSaved = fakeItems.sumOf { it.savedAmount }
//            ),
//            onGoalClick = {},
//            onCreateGoal = {},
//            onSearchQueryChange = {},
//            onStatusFilterChange = {},
//            onSortOptionChange = {},
//            onToggleSortMenu = {}
//        )
//    }
//}