// Путь в проекте: app/src/main/java/com/example/kopilka/ui/screens/statistics/StatisticsScreen.kt
//
// Графики нарисованы через Canvas без сторонних chart-библиотек — чтобы не
// плодить новые зависимости (в проекте уже были проблемы с конфликтами версий).

package com.example.kopilka.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.ui.theme.CategoryPalette
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorBorderAccent
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText2
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.utils.formatFullAmount
import com.example.kopilka.viewmodel.CategoryStat
import com.example.kopilka.viewmodel.GoalProgressItem
import com.example.kopilka.viewmodel.MonthlyPoint
import com.example.kopilka.viewmodel.StatisticsUiState
import com.example.kopilka.viewmodel.StatisticsViewModel

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(ColorBase)) {
        if (state.isLoading) return@Box

        if (!state.hasGoals) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Пока нет данных для статистики.\nСоздайте хотя бы одну цель.",
                    color = ColorText3,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Статистика", color = ColorText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Обзор всех ваших накоплений",
                color = ColorText3,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            StatGrid(state)
            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Пополнения по месяцам") {
                MonthlyBarChart(points = state.monthlyContributions)
            }
            Spacer(Modifier.height(16.dp))

            SectionCard(title = "По категориям") {
                CategoryBreakdown(stats = state.categoryBreakdown)
            }
            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Прогресс по целям") {
                GoalsProgressList(items = state.goalsProgress)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatGrid(state: StatisticsUiState) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Всего накоплено",
                value = formatFullAmount(state.totalSaved),
                accent = true
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Средний прогресс",
                value = "${state.averageProgressPercent}%"
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Активных целей",
                value = state.activeGoalsCount.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Завершено",
                value = state.completedGoalsCount.toString()
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, accent: Boolean = false) {
    Column(
        modifier = modifier
            .background(if (accent) ColorAccent.copy(alpha = 0.08f) else ColorCard, RoundedCornerShape(18.dp))
            .border(1.dp, if (accent) ColorBorderAccent else ColorBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(label, color = ColorText3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = if (accent) ColorAccent else ColorText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(title, color = ColorText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun MonthlyBarChart(points: List<MonthlyPoint>) {
    val maxAmount = points.maxOfOrNull { it.amount } ?: 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        points.forEach { point ->
            val fraction = if (maxAmount <= 0) 0f else (point.amount / maxAmount).toFloat()
            val isMax = maxAmount > 0 && point.amount == maxAmount

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((120.dp * fraction).coerceAtLeast(4.dp))
                            .background(
                                if (isMax) ColorAccent else ColorAccent.copy(alpha = 0.35f),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(point.label, color = ColorText3, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(stats: List<CategoryStat>) {
    if (stats.isEmpty()) {
        Text("Нет данных", color = ColorText3, fontSize = 13.sp)
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryDonut(stats = stats)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            stats.forEach { stat ->
                val color = CategoryPalette.colorForCategory(stat.category)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stat.category?.name ?: "Other",
                        color = ColorText2,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${stat.percent.toInt()}%",
                        color = ColorText3,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDonut(stats: List<CategoryStat>) {
    Canvas(modifier = Modifier.size(110.dp)) {
        val strokeWidth = 20.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        stats.forEach { stat ->
            val sweep = (stat.percent / 100f) * 360f
            if (sweep > 0f) {
                drawArc(
                    color = CategoryPalette.colorForCategory(stat.category),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = topLeft,
                    size = arcSize
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun GoalsProgressList(items: List<GoalProgressItem>) {
    Column {
        items.forEachIndexed { index, item ->
            val color = CategoryPalette.colorForCategory(item.category)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.name,
                        color = ColorText2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (item.isCompleted) "Готово" else "${item.progressPercent}%",
                        color = if (item.isCompleted) ColorAccent else ColorText3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(ColorSurface, RoundedCornerShape(100.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progressPercent / 100f)
                            .height(6.dp)
                            .background(if (item.isCompleted) ColorAccent else color, RoundedCornerShape(100.dp))
                    )
                }
            }
            if (index != items.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}
