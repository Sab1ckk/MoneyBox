// Путь в проекте: app/src/main/java/com/example/kopilka/viewmodel/StatisticsViewModel.kt

package com.example.kopilka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Deposit
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import com.example.kopilka.utils.calcProgressPercent
import com.example.kopilka.utils.monthShortLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class MonthlyPoint(val label: String, val amount: Double)

/** category == null → группа "Other"/без категории. */
data class CategoryStat(val category: Category?, val amount: Double, val percent: Float)

data class GoalProgressItem(
    val id: Int,
    val name: String,
    val progressPercent: Int,
    val category: Category?,
    val isCompleted: Boolean
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val hasGoals: Boolean = false,
    val totalSaved: Double = 0.0,
    val averageProgressPercent: Int = 0,
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0,
    val monthlyContributions: List<MonthlyPoint> = emptyList(),
    val categoryBreakdown: List<CategoryStat> = emptyList(),
    val goalsProgress: List<GoalProgressItem> = emptyList()
)

private data class GoalStatDetail(
    val goal: Goal,
    val savedAmount: Double,
    val deposits: List<Deposit>,
    val primaryCategory: Category?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    private val depositRepository: DepositRepository,
    private val goalCategoryRepository: GoalCategoryRepository
) : ViewModel() {

    private fun goalDetailFlow(goal: Goal): Flow<GoalStatDetail> =
        combine(
            depositRepository.getDepositsForGoal(goal.id),
            goalCategoryRepository.getCategoriesForGoal(goal.id)
        ) { deposits, categories ->
            GoalStatDetail(
                goal = goal,
                savedAmount = deposits.sumOf { it.amount },
                deposits = deposits,
                primaryCategory = categories.firstOrNull()
            )
        }

    val uiState: StateFlow<StatisticsUiState> = goalRepository.getAllGoals()
        .flatMapLatest { goals ->
            if (goals.isEmpty()) flowOf(emptyList())
            else combine(goals.map { goalDetailFlow(it) }) { it.toList() }
        }
        .map { details -> buildUiState(details) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )

    private fun buildUiState(details: List<GoalStatDetail>): StatisticsUiState {
        if (details.isEmpty()) {
            return StatisticsUiState(isLoading = false, hasGoals = false)
        }

        val totalSaved = details.sumOf { it.savedAmount }

        val activeCount = details.count { it.savedAmount < it.goal.targetAmount }
        val completedCount = details.size - activeCount

        val averageProgress = details
            .map { d -> if (d.goal.targetAmount <= 0) 0.0 else (d.savedAmount / d.goal.targetAmount * 100).coerceAtMost(100.0) }
            .average()

        // Пополнения по месяцам — последние 6 календарных месяцев, включая текущий.
        val currentMonth = YearMonth.now()
        val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val allDeposits = details.flatMap { it.deposits }
        val monthlyContributions = months.map { ym ->
            val sum = allDeposits.filter { YearMonth.from(it.date) == ym }.sumOf { it.amount }
            MonthlyPoint(label = monthShortLabel(ym), amount = sum)
        }

        // Распределение накоплений по категориям (по первой категории цели).
        val byCategory = details.groupBy { it.primaryCategory?.id }
        val categoryBreakdown = byCategory.values.map { group ->
            val amount = group.sumOf { it.savedAmount }
            CategoryStat(
                category = group.first().primaryCategory,
                amount = amount,
                percent = if (totalSaved <= 0) 0f else (amount / totalSaved * 100).toFloat()
            )
        }.sortedByDescending { it.amount }

        val goalsProgress = details
            .sortedByDescending { calcProgressPercent(it.savedAmount, it.goal.targetAmount) }
            .map { d ->
                GoalProgressItem(
                    id = d.goal.id,
                    name = d.goal.name,
                    progressPercent = calcProgressPercent(d.savedAmount, d.goal.targetAmount),
                    category = d.primaryCategory,
                    isCompleted = d.savedAmount >= d.goal.targetAmount
                )
            }

        return StatisticsUiState(
            isLoading = false,
            hasGoals = true,
            totalSaved = totalSaved,
            averageProgressPercent = averageProgress.roundToInt(),
            activeGoalsCount = activeCount,
            completedGoalsCount = completedCount,
            monthlyContributions = monthlyContributions,
            categoryBreakdown = categoryBreakdown,
            goalsProgress = goalsProgress
        )
    }
}
