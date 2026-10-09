package com.example.kopilka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.repository.BankDepositRepository
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import com.example.kopilka.utils.DeadlineForecast
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
import com.example.kopilka.data.datastore.DefaultSortSetting
import com.example.kopilka.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.repository.CategoryRepository

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
    /** основная (первая) категория цели — из неё берутся цвет и иконка*/
    val category: Category?,
    val categoryIcon: String?,
    val hasBankDeposit: Boolean,
    val interestRate: Double?,
    val isCompleted: Boolean,
    val progressPercent: Int,
    val savedAmount: Double,
    val targetAmount: Double,
    /** null — дедлайн не задан и прогноз ещё нельзя построить (нет пополнений). */
    val daysLeft: Long?,
    /** true — пользователь задал дедлайн сам; false — дата рассчитана автоматически. */
    val isDeadlineEstimated: Boolean,
    val categoryIds: Set<Int>,
)

data class GoalListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortOption: SortOption = SortOption.DEADLINE,
    val isSortMenuOpen: Boolean = false,
    val items: List<GoalListItemUi> = emptyList(),
    val totalSaved: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Int> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val depositRepository: DepositRepository,
    private val goalCategoryRepository: GoalCategoryRepository,
    private val bankDepositRepository: BankDepositRepository,
    private val settingsDataStore: SettingsDataStore,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val selectedCategoryIds = MutableStateFlow<Set<Int>>(emptySet())
    private val sortOption = MutableStateFlow(SortOption.DEADLINE)

    init {
        viewModelScope.launch {
            sortOption.value = settingsDataStore.defaultSort.first().toSortOption()
        }
    }
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

            // Дедлайн: если задан вручную — используем его. Если нет — считаем
            // автоматический прогноз по темпу пополнений (см. DeadlineForecast).
            val manualDeadline = goal.deadline
            val effectiveDeadline = manualDeadline ?: run {
                val forecast = DeadlineForecast.estimate(saved, goal.targetAmount, deposits, bankDeposit)
                (forecast as? DeadlineForecast.Estimated)?.date
            }

            GoalListItemUi(
                id = goal.id,
                name = goal.name,
                description = goal.description,
                category = primaryCategory,
                categoryIcon = primaryCategory?.icon?.ifBlank { null },
                hasBankDeposit = bankDeposit != null,
                interestRate = bankDeposit?.interestRate,
                isCompleted = saved >= goal.targetAmount,
                progressPercent = calcProgressPercent(saved, goal.targetAmount),
                savedAmount = saved,
                targetAmount = goal.targetAmount,
                daysLeft = effectiveDeadline?.let { daysUntil(it) },
                categoryIds = categories.map { it.id }.toSet(),
                isDeadlineEstimated = manualDeadline == null
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

    private val filters = combine(searchQuery, statusFilter, selectedCategoryIds) { q, s, c ->
        Triple(q, s, c)
    }
    private val sortState = combine(sortOption, isSortMenuOpen) { sort, menuOpen -> sort to menuOpen }

    val uiState: StateFlow<GoalListUiState> = combine(
        allItems, filters, sortState, categoryRepository.getAllCategories()
    ) { items, (query, status, selectedRaw), (sort, sortMenuOpen), categories ->
        // Отбрасываем id категорий, которых уже нет (например, после очистки данных)
        val selected = selectedRaw intersect categories.map { it.id }.toSet()

        val filtered = items.filter { item ->
            val matchesSearch = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)
            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> !item.isCompleted
                StatusFilter.COMPLETED -> item.isCompleted
            }
            // Пустой выбор = фильтр по категориям выключен
            val matchesCategory = selected.isEmpty() || item.categoryIds.any { it in selected }
            matchesSearch && matchesStatus && matchesCategory
        }

        val sorted = when (sort) {
            SortOption.DEADLINE -> filtered.sortedBy { it.daysLeft ?: Long.MAX_VALUE }
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
            totalSaved = items.sumOf { it.savedAmount },
            categories = categories,
            selectedCategoryIds = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalListUiState()
    )

    fun onToggleCategory(categoryId: Int) {
        selectedCategoryIds.value = selectedCategoryIds.value.let {
            if (categoryId in it) it - categoryId else it + categoryId
        }
    }

    fun onClearCategories() {
        selectedCategoryIds.value = emptySet()
    }

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

private fun DefaultSortSetting.toSortOption(): SortOption = when (this) {
    DefaultSortSetting.DEADLINE -> SortOption.DEADLINE
    DefaultSortSetting.PROGRESS -> SortOption.PROGRESS
    DefaultSortSetting.AMOUNT -> SortOption.AMOUNT
}
