// Путь в проекте: app/src/main/java/com/example/kopilka/viewmodel/GoalDetailsViewModel.kt

package com.example.kopilka.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Deposit
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.repository.BankDepositRepository
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import com.example.kopilka.utils.DeadlineForecast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GoalDetailsUiState(
    val isLoading: Boolean = true,
    val goalNotFound: Boolean = false,
    val goal: Goal? = null,
    val savedAmount: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val bankDeposit: BankDeposit? = null,
    val contributions: List<Deposit> = emptyList(),
    /** true — пользователь сам задал дедлайн; false — дата ниже (если есть) рассчитана автоматически. */
    val hasManualDeadline: Boolean = false,
    /** Дата, которую показываем пользователю: ручная либо расчётная (с приоритетом варианта "с вкладом"). */
    val effectiveDeadline: LocalDate? = null,
    /** Прогноз без учёта вклада — нужен всегда, в том числе для сравнения "было бы без вклада". */
    val forecastWithoutDeposit: DeadlineForecast? = null,
    /** Прогноз с учётом вклада — только если вклад подключён. */
    val forecastWithDeposit: DeadlineForecast? = null
) {
    val progress: Float
        get() = goal?.let {
            if (it.targetAmount <= 0) 0f else (savedAmount / it.targetAmount).toFloat().coerceIn(0f, 1f)
        } ?: 0f

    val isCompleted: Boolean
        get() = goal != null && savedAmount >= goal.targetAmount

    val remainingAmount: Double
        get() = goal?.let { (it.targetAmount - savedAmount).coerceAtLeast(0.0) } ?: 0.0

    val daysLeft: Long?
        get() = effectiveDeadline?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
}

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val depositRepository: DepositRepository,
    private val goalCategoryRepository: GoalCategoryRepository,
    private val bankDepositRepository: BankDepositRepository
) : ViewModel() {

    private val goalId: Int = checkNotNull(savedStateHandle.get<Int>("goalId")) {
        "GoalDetailsScreen требует goalId в аргументах навигации"
    }

    val uiState: StateFlow<GoalDetailsUiState> = combine(
        goalRepository.getGoalById(goalId),
        depositRepository.getDepositsForGoal(goalId),
        goalCategoryRepository.getCategoriesForGoal(goalId),
        bankDepositRepository.getByGoalId(goalId)
    ) { goal, deposits, categories, bankDeposit ->
        if (goal == null) {
            return@combine GoalDetailsUiState(isLoading = false, goalNotFound = true)
        }

        val saved = deposits.sumOf { it.amount }
        val manualDeadline = goal.deadline

        // Прогноз без вклада считаем всегда — он же используется как итоговая
        // дата, если вклад не подключён, и как база для сравнения, если подключён.
        val forecastWithoutDeposit = DeadlineForecast.estimate(saved, goal.targetAmount, deposits, bankDeposit = null)
        val forecastWithDeposit = bankDeposit?.let {
            DeadlineForecast.estimate(saved, goal.targetAmount, deposits, it)
        }

        val effectiveDeadline = manualDeadline
            ?: (forecastWithDeposit as? DeadlineForecast.Estimated)?.date
            ?: (forecastWithoutDeposit as? DeadlineForecast.Estimated)?.date

        GoalDetailsUiState(
            isLoading = false,
            goal = goal,
            savedAmount = saved,
            categories = categories,
            bankDeposit = bankDeposit,
            contributions = deposits.sortedByDescending { it.date },
            hasManualDeadline = manualDeadline != null,
            effectiveDeadline = effectiveDeadline,
            forecastWithoutDeposit = forecastWithoutDeposit,
            forecastWithDeposit = forecastWithDeposit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalDetailsUiState()
    )

    fun addFunds(amount: Double, comment: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            depositRepository.insert(
                Deposit(
                    goalId = goalId,
                    amount = amount,
                    date = LocalDate.now(),
                    comment = comment.ifBlank { "Пополнение" }
                )
            )
        }
    }

    fun deleteGoal(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState.value.goal?.let { goalRepository.delete(it) }
            onDeleted()
        }
    }
}
