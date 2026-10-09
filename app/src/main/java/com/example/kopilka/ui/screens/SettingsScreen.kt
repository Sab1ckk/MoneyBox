// Путь в проекте: app/src/main/java/com/example/kopilka/ui/screens/settings/SettingsScreen.kt

package com.example.kopilka.ui.screens.settings

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.utils.CsvBackupManager
import com.example.kopilka.data.datastore.DefaultSortSetting
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
import com.example.kopilka.viewmodel.CurrencyOption
import com.example.kopilka.viewmodel.SettingsViewModel
import com.example.kopilka.data.datastore.ThemeModeSetting
import com.example.kopilka.utils.BiometricAuthenticator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale

@Composable
fun SettingsScreen(
    onChangePin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricAvailable = remember { BiometricAuthenticator.isAvailable(context) }


    var showSortPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.setNotifications(false)
            scope.launch { snackbarHostState.showSnackbar("Разрешите уведомления в настройках системы") }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val success = context.contentResolver.openOutputStream(uri)?.use { stream ->
                viewModel.exportData(stream)
            } ?: false
            snackbarHostState.showSnackbar(
                if (success) "Резервная копия сохранена" else "Не удалось экспортировать данные"
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = context.contentResolver.openInputStream(uri)?.use { stream ->
                viewModel.importData(stream)
            }
            val message = when (result) {
                is CsvBackupManager.ImportResult.Success ->
                    "Импортировано: ${result.goals} целей, ${result.deposits} пополнений"
                is CsvBackupManager.ImportResult.Error -> result.message
                null -> "Не удалось открыть файл"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = ColorBase,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorBase)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Настройки", color = ColorText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

            SectionHeader("Внешний вид")
            SettingsCard {
                SettingItem(
                    icon = "🎨",
                    label = "Тема",
                    sub = state.themeMode.label(),
                    onClick = { showThemePicker = true }
                )
            }

            SectionHeader("Общие")
            SettingsCard {
                SettingItem(
                    icon = "↕",
                    label = "Сортировка по умолчанию",
                    sub = state.defaultSort.label(),
                    onClick = { showSortPicker = true }
                )
//                Divider()
//                SettingItem(
//                    icon = "₽",
//                    label = "Валюта",
//                    sub = state.currency.label,
//                    onClick = { showCurrencyPicker = true }
//                )
            }

            SectionHeader("Уведомления")
            SettingsCard {
                SettingItem(
                    icon = "🔔",
                    label = "Уведомления",
                    sub = "Включить push-уведомления",
                    toggle = state.notifications to { on ->
                        viewModel.setNotifications(on)
                        if (on && ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                if (state.notifications) {
                    Divider()
                    SettingItem(
                        icon = "📅",
                        label = "Напоминания о дедлайне",
                        sub = "За 30 дней до срока",
                        toggle = state.deadlineAlerts to viewModel::setDeadlineAlerts
                    )
//                    Divider()
//                    SettingItem(
//                        icon = "📊",
//                        label = "Еженедельный отчёт",
//                        sub = "Сводка по воскресеньям",
//                        toggle = state.weeklyReport to viewModel::setWeeklyReport
//                    )
                }
            }

            SectionHeader("Безопасность")
            SettingsCard {
                SettingItem(
                    icon = "🔒",
                    label = "Изменить PIN",
                    sub = "Обновить 4-значный код доступа",
                    onClick = { showChangePinDialog = true }
                )
                if (biometricAvailable){
                    Divider()
                    SettingItem(
                        icon = "👆",
                        label = "Биометрическая разблокировка",
                        sub = "Отпечаток пальца или Face ID",
                        toggle = state.biometric to viewModel::setBiometric
                    )
                }
            }

            SectionHeader("Данные")
            SettingsCard {
                SettingItem(
                    icon = "⬇",
                    label = "Экспорт данных",
                    sub = "Сохранить все цели в CSV",
                    onClick = {
                        val fileName = "kopilka_backup_" +
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + ".csv"
                        exportLauncher.launch(fileName)
                    }
                )
                Divider()
                SettingItem(
                    icon = "⬆",
                    label = "Импорт данных",
                    sub = "Восстановить из резервной копии",
                    onClick = {
                        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
                    }
                )
            }

            SectionHeader("Опасная зона")
            SettingsCard {
                SettingItem(
                    icon = "🗑",
                    label = "Очистить все данные",
                    sub = "Удалить все цели и историю",
                    danger = true,
                    onClick = { showClearDataDialog = true }
                )
            }

            Spacer(Modifier.height(8.dp))
            AboutBlock()
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showThemePicker) {
        PickerSheet(
            title = "Тема",
            options = ThemeModeSetting.entries.map { it to it.label() },
            selected = state.themeMode,
            onSelect = { viewModel.setThemeMode(it); showThemePicker = false },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showSortPicker) {
        PickerSheet(
            title = "Сортировка по умолчанию",
            options = DefaultSortSetting.entries.map { it to it.label() },
            selected = state.defaultSort,
            onSelect = { viewModel.setDefaultSort(it); showSortPicker = false },
            onDismiss = { showSortPicker = false }
        )
    }

    if (showCurrencyPicker) {
        PickerSheet(
            title = "Валюта",
            options = CurrencyOption.entries.map { it to it.label },
            selected = state.currency,
            onSelect = { viewModel.setCurrency(it); showCurrencyPicker = false },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    if (showChangePinDialog) {
        ConfirmDialog(
            icon = "🔒",
            title = "Изменить PIN-код?",
            message = "Текущий PIN будет сброшен, и вам нужно будет придумать новый код доступа.",
            confirmLabel = "Изменить",
            onConfirm = {
                showChangePinDialog = false
                scope.launch {
                    viewModel.changePin()
                    onChangePin()
                }
            },
            onCancel = { showChangePinDialog = false }
        )
    }

    if (showClearDataDialog) {
        ConfirmDialog(
            icon = "🗑",
            title = "Удалить все данные?",
            message = "Все цели, категории, пополнения и вклады будут удалены безвозвратно. Это действие нельзя отменить.",
            confirmLabel = "Удалить всё",
            danger = true,
            onConfirm = {
                showClearDataDialog = false
                scope.launch {
                    viewModel.clearAllData()
                    snackbarHostState.showSnackbar("Все данные удалены")
                }
            },
            onCancel = { showClearDataDialog = false }
        )
    }
}

private fun DefaultSortSetting.label(): String = when (this) {
    DefaultSortSetting.DEADLINE -> "Дедлайн"
    DefaultSortSetting.PROGRESS -> "Прогресс"
    DefaultSortSetting.AMOUNT -> "Сумма"
}

private fun ThemeModeSetting.label(): String = when (this) {
    ThemeModeSetting.LIGHT -> "Светлая"
    ThemeModeSetting.DARK -> "Тёмная"
    ThemeModeSetting.SYSTEM -> "Как в системе"
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = ColorText3,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorCard, RoundedCornerShape(20.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(ColorBorder)
    )
}

@Composable
private fun SettingItem(
    icon: String,
    label: String,
    sub: String? = null,
    danger: Boolean = false,
    trailingBadge: String? = null,
    toggle: Pair<Boolean, (Boolean) -> Unit>? = null,
    onClick: (() -> Unit)? = null
) {
    val isAccented = toggle?.first == true
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (danger) ColorErrorDim else if (isAccented) ColorAccentDim else ColorSurface,
                    RoundedCornerShape(11.dp)
                )
                .border(
                    1.dp,
                    if (danger) Color(0x26EF4444) else if (isAccented) ColorBorderAccent else ColorBorder,
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (danger) ColorError else if (isAccented) ColorAccent else ColorText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (sub != null) {
                Text(text = sub, color = ColorText3, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        when {
            toggle != null -> SettingsToggle(value = toggle.first, onChange = toggle.second)
            trailingBadge != null -> Text(
                text = trailingBadge,
                color = ColorText3,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(ColorSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            onClick != null -> Text("›", color = ColorText3, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsToggle(value: Boolean, onChange: (Boolean) -> Unit) {
    val offset by animateDpAsState(
        targetValue = if (value) 21.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 500f
        ),
        label = "ToggleOffset"
    )

    // Анимация цвета фона
    val backgroundColor by animateColorAsState(
        targetValue = if (value) ColorAccent else ColorSurface,
        animationSpec = tween(durationMillis = 220),
        label = "ToggleBackground"
    )

    // Анимация цвета границы
    val borderColor by animateColorAsState(
        targetValue = if (value) ColorAccent else ColorBorder,
        animationSpec = tween(durationMillis = 220),
        label = "ToggleBorder"
    )

    // Небольшой эффект увеличения бегунка
    val scale by animateFloatAsState(
        targetValue = if (value) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "ToggleScale"
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(100.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(100.dp)
            )
            .clip(RoundedCornerShape(100.dp))
            .clickable { onChange(!value) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = offset)
                .size(18.dp)
                .scale(scale)
                .background(
                    color = if (value) ColorBase else ColorText3,
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

@Composable
private fun <T> PickerSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .background(ColorSurface, RoundedCornerShape(100.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text(title, color = ColorText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .background(
                            if (isSelected) ColorAccentDim else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) ColorAccent else ColorText2,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSelected) {
                        Text("✓", color = ColorAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConfirmDialog(
    icon: String,
    title: String,
    message: String,
    confirmLabel: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onCancel)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(ColorSurface, RoundedCornerShape(100.dp))
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (danger) ColorErrorDim else ColorAccentDim, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = ColorText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = ColorText3,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Отмена",
                    color = ColorText2,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 14.dp)
                )
                Text(
                    text = confirmLabel,
                    color = if (danger) Color.White else ColorBase,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (danger) ColorError else ColorAccent, RoundedCornerShape(14.dp))
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutBlock() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(ColorAccentDim, RoundedCornerShape(14.dp))
                .border(1.dp, ColorBorderAccent, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("💰", fontSize = 20.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("Копилка", color = ColorText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "Версия 1.0.0 · для Android",
            color = ColorText3,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
