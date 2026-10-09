package com.example.kopilka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.utils.CsvBackupManager
import com.example.kopilka.data.datastore.DefaultSortSetting
import com.example.kopilka.data.datastore.PinDataStore
import com.example.kopilka.data.datastore.SettingsDataStore
import com.example.kopilka.data.datastore.ThemeModeSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

enum class CurrencyOption(val symbol: String, val label: String) {
    RUB("₽", "₽ RUB"),
    USD("$", "$ USD"),
    EUR("€", "€ EUR");

    companion object {
        fun fromSymbol(symbol: String): CurrencyOption =
            entries.firstOrNull { it.symbol == symbol } ?: RUB
    }
}

data class SettingsUiState(
    val notifications: Boolean = true,
    val deadlineAlerts: Boolean = true,
    val weeklyReport: Boolean = false,
    val biometric: Boolean = false,
    val defaultSort: DefaultSortSetting = DefaultSortSetting.DEADLINE,
    val currency: CurrencyOption = CurrencyOption.RUB,
    val themeMode: ThemeModeSetting = ThemeModeSetting.DARK
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val pinDataStore: PinDataStore,
    private val csvBackupManager: CsvBackupManager
) : ViewModel() {

    private val togglesFlow = combine(
        settingsDataStore.notificationsEnabled,
        settingsDataStore.deadlineAlertsEnabled,
        settingsDataStore.weeklyReportEnabled,
        settingsDataStore.biometricEnabled
    ) { notifications, deadlineAlerts, weeklyReport, biometric ->
        listOf(notifications, deadlineAlerts, weeklyReport, biometric)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        togglesFlow,
        settingsDataStore.defaultSort,
        settingsDataStore.currencySymbol,
        settingsDataStore.themeMode
    ) { toggles, sort, currency, theme ->
        SettingsUiState(
            notifications = toggles[0],
            deadlineAlerts = toggles[1],
            //weeklyReport = toggles[2],
            biometric = toggles[3],
            defaultSort = sort,
            currency = CurrencyOption.fromSymbol(currency),
            themeMode = theme
        )
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState())

    fun setNotifications(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotificationsEnabled(value) }
    }

    fun setDeadlineAlerts(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setDeadlineAlertsEnabled(value) }
    }

    fun setWeeklyReport(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setWeeklyReportEnabled(value) }
    }

    fun setBiometric(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setBiometricEnabled(value) }
    }

    fun setDefaultSort(value: DefaultSortSetting) {
        viewModelScope.launch { settingsDataStore.setDefaultSort(value) }
    }

    fun setCurrency(value: CurrencyOption) {
        viewModelScope.launch { settingsDataStore.setCurrencySymbol(value.symbol) }
    }

    fun setThemeMode(value: ThemeModeSetting) {
        viewModelScope.launch { settingsDataStore.setThemeMode(value) }
    }

    /** Сбрасывает сохранённый PIN — после этого экран PIN снова покажет создание кода. */
    suspend fun changePin() {
        pinDataStore.clearPin()
    }

    suspend fun clearAllData() {
        csvBackupManager.clearAllData()
    }

    suspend fun exportData(outputStream: OutputStream): Boolean =
        runCatching { csvBackupManager.exportTo(outputStream) }.isSuccess

    suspend fun importData(inputStream: InputStream): CsvBackupManager.ImportResult =
        csvBackupManager.importFrom(inputStream)
}
