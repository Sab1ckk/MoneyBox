package com.example.kopilka.ui.screens.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.window.Dialog
import com.example.kopilka.data.database.DefaultCategories
import com.example.kopilka.data.entity.Category
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.ui.components.AddCategoryDialog
import com.example.kopilka.ui.theme.CategoryPalette
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorAccentDim
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorBorderAccent
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorElevated
import com.example.kopilka.ui.theme.ColorError
import com.example.kopilka.ui.theme.ColorErrorDim
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText2
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.utils.formatDate
import com.example.kopilka.viewmodel.CreateGoalUiState
import com.example.kopilka.viewmodel.CreateGoalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val CAPITALIZATION_OPTIONS = listOf(1 to "Ежемесячно", 3 to "Ежеквартально", 12 to "Ежегодно")

@Composable
fun CreateGoalScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateGoalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onSaved()
    }

    CreateGoalContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onTargetAmountChange = viewModel::onTargetAmountChange,
        onDeadlineChange = viewModel::onDeadlineChange,
        onToggleCategory = viewModel::toggleCategory,
        onOpenAddCategory = viewModel::openAddCategoryDialog,
        onRequestDeleteCategory = viewModel::requestDeleteCategory,
        onDepositToggle = viewModel::onDepositToggle,
        onInterestRateChange = viewModel::onInterestRateChange,
        onCapitalizationChange = viewModel::onCapitalizationChange,
        onInitialAmountChange = viewModel::onInitialAmountChange,
        onOpeningDateSelected = viewModel::onOpeningDateSelected,
        onSave = viewModel::save,
        onDismissSnackbar = viewModel::dismissSnackbar
    )

    if (state.isAddCategoryDialogOpen) {
        AddCategoryDialog(
            name = state.newCategoryName,
            selectedColorHex = state.newCategoryColorHex,
            onNameChange = viewModel::onNewCategoryNameChange,
            onColorSelected = viewModel::onNewCategoryColorSelected,
            onConfirm = viewModel::confirmAddCategory,
            onDismiss = viewModel::dismissAddCategoryDialog
        )
    }

    state.categoryToDelete?.let { category ->
        DeleteCategoryDialog(
            categoryName = category.name,
            onConfirm = viewModel::confirmDeleteCategory,
            onDismiss = viewModel::dismissDeleteCategory
        )
    }
}

@Composable
private fun CreateGoalContent(
    state: CreateGoalUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTargetAmountChange: (String) -> Unit,
    onDeadlineChange: (LocalDate?) -> Unit,
    onToggleCategory: (Int) -> Unit,
    onOpenAddCategory: () -> Unit,
    onRequestDeleteCategory: (Category) -> Unit,
    onDepositToggle: (Boolean) -> Unit,
    onInterestRateChange: (String) -> Unit,
    onCapitalizationChange: (Int) -> Unit,
    onInitialAmountChange: (String) -> Unit,
    onOpeningDateSelected: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismissSnackbar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBase)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ColorSurface, RoundedCornerShape(11.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = ColorText2)
                }
                Text(
                    text = if (state.isEditMode) "Редактировать цель" else "Новая цель",
                    color = ColorText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                FieldLabel("Название цели")
                FormTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = "Например, Поездка в Японию",
                    error = state.errors.name
                )
                Spacer(Modifier.height(20.dp))

                FieldLabel("Описание (необязательно)")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    BasicTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        textStyle = TextStyle(color = ColorText, fontSize = 15.sp),
                        cursorBrush = SolidColor(ColorAccent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (state.description.isEmpty()) {
                                Text("Пара слов о цели...", color = ColorText3, fontSize = 15.sp)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(20.dp))

                FieldLabel("Целевая сумма")
                FormTextField(
                    value = state.targetAmount,
                    onValueChange = onTargetAmountChange,
                    placeholder = "0",
                    error = state.errors.targetAmount,
                    keyboardType = KeyboardType.Number,
                    suffix = "₽"
                )
                Spacer(Modifier.height(20.dp))

                FieldLabel("Дедлайн (необязательно)")
                DateField(
                    selectedDate = state.deadline,
                    onDateSelected = onDeadlineChange,
                    onClear = { onDeadlineChange(null) },
                )
                Text(
                    text = "Если не указать — дата будет рассчитываться автоматически по темпу ваших пополнений",
                    color = ColorText3,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(20.dp))

                FieldLabel("Категория")
                CategoryChips(
                    categories = state.allCategories,
                    selectedIds = state.selectedCategoryIds,
                    onToggle = onToggleCategory,
                    onAddClick = onOpenAddCategory,
                    onDeleteClick = onRequestDeleteCategory
                )
                Spacer(Modifier.height(24.dp))

                DepositSection(
                    state = state,
                    onDepositToggle = onDepositToggle,
                    onInterestRateChange = onInterestRateChange,
                    onCapitalizationChange = onCapitalizationChange,
                    onInitialAmountChange = onInitialAmountChange,
                    onOpeningDateSelected = onOpeningDateSelected
                )
                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorAccent, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !state.isSaving, onClick = onSave)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isEditMode) "Сохранить изменения" else "Создать цель",
                        color = ColorBase,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Кнопка "Отмена" — выход без сохранения
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !state.isSaving, onClick = onBack)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Отмена",
                        color = ColorText2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))


                if (state.snackbarMessage != null) {
                    SnackbarError(
                        message = state.snackbarMessage,
                        onDismiss = onDismissSnackbar
                    )
                }

            }
        }

    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = ColorText3,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurface, RoundedCornerShape(14.dp))
                .border(1.dp, if (error != null) ColorError else ColorBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = ColorText, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(ColorAccent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = ColorText3, fontSize = 15.sp)
                    }
                    inner()
                }
            )
            if (suffix != null) {
                Text(suffix, color = ColorText3, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = ColorError,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    error: String? = null,
    onClear: (() -> Unit)? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(ColorSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, if (error != null) ColorError else ColorBorder, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Text(
                    text = selectedDate?.let { formatDate(it) } ?: "Выбрать дату",
                    color = if (selectedDate != null) ColorText else ColorText3,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (selectedDate != null && onClear != null) {
                Text(
                    text = "Сбросить",
                    color = ColorError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClear)
                        .padding(start = 12.dp)
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = ColorError,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }

    if (showPicker) {
        val initialMillis = selectedDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showPicker = false
                }) { Text("Ок", color = ColorAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена", color = ColorText3) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<com.example.kopilka.data.entity.Category>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Category) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val selected = category.id in selectedIds
            val color = CategoryPalette.colorForCategory(category)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onToggle(category.id) }
                    .background(
                        if (selected) color.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(100.dp)
                    )
                    .border(
                        1.dp,
                        if (selected) color else ColorBorder,
                        RoundedCornerShape(100.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(category.icon.ifBlank { "●" }, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    category.name,
                    color = if (selected) color else ColorText3,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!DefaultCategories.isDefault(category)) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "×",
                        color = ColorText3,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDeleteClick(category) }
                    )
                }
            }
        }

        // "+ Своя категория" — отдельная кнопка, открывает диалог
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .clickable(onClick = onAddClick)
                .border(1.dp, ColorBorderAccent, RoundedCornerShape(100.dp))
                .background(ColorAccentDim, RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("+", color = ColorAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("Своя категория", color = ColorAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DepositSection(
    state: CreateGoalUiState,
    onDepositToggle: (Boolean) -> Unit,
    onInterestRateChange: (String) -> Unit,
    onCapitalizationChange: (Int) -> Unit,
    onInitialAmountChange: (String) -> Unit,
    onOpeningDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (state.depositEnabled) ColorBorderAccent else ColorBorder,
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onDepositToggle(!state.depositEnabled) }
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Подключить банковский вклад", color = ColorText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Учитывать капитализацию процентов при прогнозе",
                    color = ColorText3,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            val switchOffset by animateDpAsState(
                targetValue = if (state.depositEnabled) 18.dp else 0.dp,
                animationSpec = tween(durationMillis = 200),
                label = "switchOffset"
            )

            val switchBackground by animateColorAsState(
                targetValue = if (state.depositEnabled) ColorAccent else ColorSurface,
                animationSpec = tween(durationMillis = 200),
                label = "switchBackground"
            )

            val switchBorder by animateColorAsState(
                targetValue = if (state.depositEnabled) ColorAccent else ColorBorder,
                animationSpec = tween(durationMillis = 200),
                label = "switchBorder"
            )

            val switchThumb by animateColorAsState(
                targetValue = if (state.depositEnabled) ColorBase else ColorText3,
                animationSpec = tween(durationMillis = 200),
                label = "switchThumb"
            )

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(26.dp)
                    .background(
                        color = switchBackground,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = switchBorder,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = switchOffset)
                        .size(18.dp)
                        .background(
                            color = switchThumb,
                            shape = CircleShape
                        )
                )
            }
        }

        if (state.depositEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                FieldLabel("Процентная ставка (% годовых)")
                FormTextField(
                    value = state.interestRate,
                    onValueChange = onInterestRateChange,
                    placeholder = "Например, 16",
                    error = state.errors.interestRate,
                    keyboardType = KeyboardType.Number,
                    suffix = "%"
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("Период капитализации")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    CAPITALIZATION_OPTIONS.forEach { (months, label) ->
                        val selected = state.capitalizationMonths == months
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCapitalizationChange(months) }
                                .background(
                                    if (selected) ColorElevated else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) ColorAccent else ColorText3,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                FieldLabel("Начальная сумма вклада")
                FormTextField(
                    value = state.initialAmount,
                    onValueChange = onInitialAmountChange,
                    placeholder = "0",
                    error = state.errors.initialAmount,
                    keyboardType = KeyboardType.Number,
                    suffix = "₽"
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("Дата открытия")
                DateField(
                    selectedDate = state.openingDate,
                    onDateSelected = onOpeningDateSelected
                )
            }
        }
    }
}

@Composable
private fun SnackbarError(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss)
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorErrorDim.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .border(1.dp, ColorError.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(message, color = ColorText2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    "×",
                    color = ColorText3,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}

@Composable
private fun DeleteCategoryDialog(categoryName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                "Удалить категорию «$categoryName»?",
                color = ColorText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Категория будет убрана у всех целей. Сами цели и пополнения останутся.",
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
