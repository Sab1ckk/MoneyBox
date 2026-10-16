package com.example.kopilka.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.data.entity.Deposit
import com.example.kopilka.ui.theme.CategoryPalette
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorAccentDim
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorBorderAccent
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorError
import com.example.kopilka.ui.theme.ColorErrorDim
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText2
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.utils.DeadlineForecast
import com.example.kopilka.utils.formatDate
import com.example.kopilka.utils.formatFullAmount
import com.example.kopilka.viewmodel.GoalDetailsUiState
import com.example.kopilka.viewmodel.GoalDetailsViewModel
import com.example.kopilka.utils.RequiredDailyAmount
import com.example.kopilka.ui.components.PayoutCalendarDialog
import com.example.kopilka.ui.components.DepositInfoCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager


@Composable
fun GoalDetailsScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: GoalDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddFundsSheet by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(ColorBase))
        return
    }
    if (state.goalNotFound || state.goal == null) {
        Box(Modifier.fillMaxSize().background(ColorBase), contentAlignment = Alignment.Center) {
            Text("Цель не найдена", color = ColorText3, fontSize = 15.sp)
        }
        return
    }

    val goal = state.goal!!

    Box(modifier = Modifier.fillMaxSize().background(ColorBase)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp).background(ColorSurface, RoundedCornerShape(11.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = ColorText2)
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { onEdit(goal.id) },
                    modifier = Modifier.size(36.dp).background(ColorSurface, RoundedCornerShape(11.dp))
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = ColorText2)
                }
                Spacer(Modifier.width(20.dp))
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp).background(ColorErrorDim, RoundedCornerShape(11.dp))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = ColorError)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                val categoryColor = CategoryPalette.colorForCategory(state.categories.firstOrNull())


                Text(goal.name, color = ColorText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                if (goal.description.isNotBlank()) {
                    Text(
                        goal.description,
                        color = ColorText3,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                state.bankDeposit?.let { deposit ->
                    Text(
                        text = "Вклад ${formatRate(deposit.interestRate)}% годовых",
                        color = ColorAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable{ showDepositDialog = true }
                            .background(ColorAccentDim, RoundedCornerShape(8.dp))
                            .border(1.dp, ColorBorderAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                ProgressCard(state = state, accentColor = categoryColor)
                Spacer(Modifier.height(16.dp))

                ForecastCard(state = state)
                Spacer(Modifier.height(16.dp))

                ContributionsCard(contributions = state.contributions)
                Spacer(Modifier.height(24.dp))
            }

            if (!state.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .background(ColorAccent, RoundedCornerShape(16.dp))
                        .clickable { showAddFundsSheet = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Пополнить", color = ColorBase, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            goalName = goal.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteGoal(onDeleted = onBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showAddFundsSheet) {
        AddFundsDialog(
            onAdd = { amount, comment -> viewModel.addFunds(amount, comment) },
            onDismiss = { showAddFundsSheet = false }
        )
    }

    if (showDepositDialog) {
        DepositInfoCard(
            deposit = state.bankDeposit!!,
            onDismiss = { showDepositDialog = false }
        )
    }
}

@Composable
private fun ProgressCard(state: GoalDetailsUiState, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(24.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            ProgressRing(progress = state.progress, color = accentColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(state.progress * 100).toInt()}%",
                    color = if (state.isCompleted) ColorAccent else ColorText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (state.isCompleted) {
                    Text("Готово!", color = ColorAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Накоплено", color = ColorText3, fontSize = 12.sp)
            Text(
                formatFullAmount(state.savedAmount),
                color = if (state.isCompleted) ColorAccent else ColorText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(10.dp))
            Text("Цель", color = ColorText3, fontSize = 11.sp)
            Text(
                formatFullAmount(state.goal!!.targetAmount),
                color = ColorText2, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )

            if (!state.isCompleted) {
                Spacer(Modifier.width(18.dp))

            Text("Осталось", color = ColorText3, fontSize = 11.sp)
            Text(
                formatFullAmount(state.remainingAmount),
                color = ColorText2, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(112.dp)) {
        val stroke = 11.dp.toPx()
        drawArc(
            color = color.copy(alpha = 0.15f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            size = Size(size.width - stroke, size.height - stroke),
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            size = Size(size.width - stroke, size.height - stroke),
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
        )
    }
}

/**
 * Блок прогноза даты завершения цели:
 * - дедлайн задан вручную → просто показываем его;
 * - не задан, пополнений ещё нет → просим сделать первое пополнение;
 * - не задан, есть пополнения → показываем расчётную дату по темпу
 *   накоплений, а если подключён вклад — сравнение "без вклада / с вкладом".
 */
@Composable
private fun ForecastCard(state: GoalDetailsUiState) {
    val goal = state.goal ?: return
    var showCalendar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(1.dp, ColorBorderAccent, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "ПРОГНОЗ ЗАВЕРШЕНИЯ",
            color = ColorAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            goal.deadline != null -> {
                Text(
                    text = "Дедлайн установлен вручную",
                    color = ColorText3,
                    fontSize = 12.sp
                )
                Text(
                    text = formatDate(goal.deadline),
                    color = ColorText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
//
                var required = DeadlineForecast.requiredDailyAmount(
                    savedAmount = state.savedAmount,
                    targetAmount = goal.targetAmount,
                    deadline = goal.deadline,
                    bankDeposit = state.bankDeposit
                )

                Spacer(Modifier.height(10.dp))
                when(required){
                    is RequiredDailyAmount.Required -> {
                        Text(
                            text = "Вносите ~${formatFullAmount(required.amountPerDay)} в день, чтобы успеть",
                            color = ColorAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    RequiredDailyAmount.AlreadyReached -> {
                        Text(
                            "Сумма уже накоплена 🎉",
                            color = ColorAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    RequiredDailyAmount.DeadlinePassed -> {
                        Text(
                            "Дедлайн уже наступил",
                            color = ColorError,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            state.forecastWithoutDeposit is DeadlineForecast.NoContributionsYet -> {
                Text(
                    text = "Сделайте первое пополнение — тогда мы рассчитаем примерную дату по вашему темпу накоплений",
                    color = ColorText3,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            state.forecastWithoutDeposit is DeadlineForecast.AlreadyReached -> {
                Text(text = "Цель уже накоплена 🎉", color = ColorAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            state.forecastWithoutDeposit is DeadlineForecast.CannotEstimate -> {
                Text(text = "Недостаточно данных для прогноза", color = ColorText3, fontSize = 13.sp)
            }

            else -> {
                val withoutDeposit = state.forecastWithoutDeposit as DeadlineForecast.Estimated
                val withDeposit = state.forecastWithDeposit as? DeadlineForecast.Estimated

                Text(
                    text = "Если продолжите откладывать ~${formatFullAmount(withoutDeposit.avgMonthlyAmount)} в месяц:",
                    color = ColorText3,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))

                if (withDeposit != null && state.bankDeposit != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showCalendar = true }
                    ) {
                        ForecastColumn(
                            label = "БЕЗ ПРОЦЕНТОВ",
                            date = withoutDeposit.date,
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                        ForecastColumn(
                            label = "С ПРОЦЕНТАМИ",
                            date = withDeposit.date,
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "Нажмите, чтобы открыть календарь выплат",
                        color = ColorText3,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (showCalendar) {
                        PayoutCalendarDialog(
                            deposit = state.bankDeposit,
                            endDate = withDeposit.date,
                            onDismiss = { showCalendar = false }
                        )
                    }
                }
                else {
                    Text(
                        text = formatDate(withoutDeposit.date),
                        color = ColorText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastColumn(label: String, date: java.time.LocalDate, highlighted: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(if (highlighted) ColorAccentDim else ColorSurface, RoundedCornerShape(14.dp))
            .border(1.dp, if (highlighted) ColorBorderAccent else ColorBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (highlighted) ColorAccent else ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            formatDate(date),
            color = if (highlighted) ColorAccent else ColorText2,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ContributionsCard(contributions: List<Deposit>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            "История пополнений",
            color = ColorText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (contributions.isEmpty()) {
            Text("Пополнений пока нет", color = ColorText3, fontSize = 13.sp)
        } else {
            contributions.forEach { deposit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(deposit.comment.ifBlank { "Пополнение" }, color = ColorText2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatDate(deposit.date), color = ColorText3, fontSize = 11.sp)
                    }
                    Text(
                        "+${formatFullAmount(deposit.amount)}",
                        color = ColorAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Удалить «$goalName»?", color = ColorText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "Это действие необратимо. Все пополнения по цели тоже будут удалены.",
                color = ColorText3,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Отмена",
                    color = ColorText3,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp)
                )
                Text(
                    "Удалить",
                    color = ColorError,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onConfirm).padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun AddFundsDialog(onAdd: (Double, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .background(ColorCard, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Пополнить цель", color = ColorText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    textStyle = TextStyle(color = ColorText, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(ColorAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) Text("Сумма", color = ColorText3, fontSize = 16.sp)
                        inner()
                    }
                )
                Text("₽", color = ColorText3, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                BasicTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    singleLine = true,
                    textStyle = TextStyle(color = ColorText, fontSize = 14.sp),
                    cursorBrush = SolidColor(ColorAccent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (comment.isEmpty()) Text("Комментарий (необязательно)", color = ColorText3, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            Spacer(Modifier.height(20.dp))

            val amountValue = amount.toDoubleOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorAccent, RoundedCornerShape(14.dp))
                    .clickable(enabled = amountValue != null && amountValue > 0) {
                        onAdd(amountValue!!, comment)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Пополнить", color = ColorBase, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


private fun formatRate(rate: Double): String =
    if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()

