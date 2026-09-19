package com.example.kopilka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.repository.BankDepositRepository
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import com.example.kopilka.utils.calcProgressPercent
import com.example.kopilka.utils.daysUntil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    COMPLETED("Завершённые")
}

enum class SortOption(val label: String) {
    DEADLINE("Дедлайн"),
    PROGRESS("Прогресс"),
    AMOUNT("Сумма")
}

/** Готовая для UI модель карточки цели — Goal + данные из связанных таблиц. */
data class GoalListItemUi(
    val id: Int,
    val name: String,
    val description: String,
    val categoryColorIndex: Int?,   // categories.firstOrNull()?.id — цвет по индексу, у Category нет своего цвета
    val categoryIcon: String?,
    val hasBankDeposit: Boolean,
    val interestRate: Double?,
    val isCompleted: Boolean,
    val progressPercent: Int,
    val savedAmount: Double,
    val targetAmount: Double,
    val daysLeft: Long
)

data class GoalListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortOption: SortOption = SortOption.DEADLINE,
    val isSortMenuOpen: Boolean = false,
    val items: List<GoalListItemUi> = emptyList(),
    val totalSaved: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val depositRepository: DepositRepository,
    private val goalCategoryRepository: GoalCategoryRepository,
    private val bankDepositRepository: BankDepositRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val sortOption = MutableStateFlow(SortOption.DEADLINE)
    private val isSortMenuOpen = MutableStateFlow(false)

    /**
     * savedAmount у Goal не хранится — это сумма Deposit по goalId.
     * Поэтому на каждую цель подписываемся на её пополнения, категории и вклад
     * и пересобираем карточку при изменении любого из источников.
     */
    private fun goalDetailsFlow(goal: Goal): Flow<GoalListItemUi> =
        combine(
            depositRepository.getDepositsForGoal(goal.id),
            goalCategoryRepository.getCategoriesForGoal(goal.id),
            bankDepositRepository.getByGoalId(goal.id)
        ) { deposits, categories, bankDeposit ->
            val saved = deposits.sumOf { it.amount }
            val primaryCategory = categories.firstOrNull()
            GoalListItemUi(
                id = goal.id,
                name = goal.name,
                description = goal.description,
                categoryColorIndex = primaryCategory?.id,
                categoryIcon = primaryCategory?.icon?.ifBlank { null },
                hasBankDeposit = bankDeposit != null,
                interestRate = bankDeposit?.interestRate,
                isCompleted = saved >= goal.targetAmount,
                progressPercent = calcProgressPercent(saved, goal.targetAmount),
                savedAmount = saved,
                targetAmount = goal.targetAmount,
                daysLeft = daysUntil(goal.deadline)
            )
        }

    private val allItems: Flow<List<GoalListItemUi>> =
        goalRepository.getAllGoals().flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(goals.map { goalDetailsFlow(it) }) { it.toList() }
            }
        }

    val uiState: StateFlow<GoalListUiState> = combine(
        allItems, searchQuery, statusFilter, sortOption, isSortMenuOpen
    ) { items, query, status, sort, sortMenuOpen ->
        val filtered = items.filter { item ->
            val matchesSearch = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> !item.isCompleted
                StatusFilter.COMPLETED -> item.isCompleted
            }
            matchesSearch && matchesStatus
        }

        val sorted = when (sort) {
            SortOption.DEADLINE -> filtered.sortedBy { it.daysLeft }
            SortOption.PROGRESS -> filtered.sortedByDescending { it.progressPercent }
            SortOption.AMOUNT -> filtered.sortedByDescending { it.targetAmount }
        }

        GoalListUiState(
            isLoading = false,
            searchQuery = query,
            statusFilter = status,
            sortOption = sort,
            isSortMenuOpen = sortMenuOpen,
            items = sorted,
            totalSaved = items.sumOf { it.savedAmount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalListUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterChange(filter: StatusFilter) {
        statusFilter.value = filter
    }

    fun onSortOptionChange(option: SortOption) {
        sortOption.value = option
        isSortMenuOpen.value = false
    }

    fun toggleSortMenu() {
        isSortMenuOpen.value = !isSortMenuOpen.value
    }

    /** Не используется на этом экране (удаление — на экране деталей), но нужна выше по стеку. */
    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { goalRepository.delete(goal) }
    }
}
