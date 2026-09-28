package com.example.kopilka.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.entity.GoalCategory
import com.example.kopilka.data.repository.BankDepositRepository
import com.example.kopilka.data.repository.CategoryRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import com.example.kopilka.ui.theme.CategoryPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CreateGoalErrors(
    val name: String? = null,
    val targetAmount: String? = null,
    val interestRate: String? = null,
    val initialAmount: String? = null
)

data class CreateGoalUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val name: String = "",
    val description: String = "",
    val targetAmount: String = "",
    /** null — дедлайн не задан, дата будет рассчитываться автоматически по темпу пополнений. */
    val deadline: LocalDate? = null,
    val allCategories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Int> = emptySet(),
    val depositEnabled: Boolean = false,
    val interestRate: String = "",
    /** 1 = ежемесячно, 3 = ежеквартально, 12 = раз в год. */
    val capitalizationMonths: Int = 1,
    val initialAmount: String = "",
    val openingDate: LocalDate? = null,
    val errors: CreateGoalErrors = CreateGoalErrors(),
    val snackbarMessage: String? = null,
    val isAddCategoryDialogOpen: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryColorHex: String = CategoryPalette.toHex(CategoryPalette.PRESET_COLORS.first()),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false
)

@HiltViewModel
class CreateGoalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val goalCategoryRepository: GoalCategoryRepository,
    private val bankDepositRepository: BankDepositRepository
) : ViewModel() {

    // route: "createGoal?goalId={goalId}" — -1 (или отсутствие аргумента) означает создание новой цели
    private val goalId: Int = savedStateHandle.get<Int>("goalId")?.takeIf { it > 0 } ?: -1
    private val isEditMode = goalId > 0

    private val _uiState = MutableStateFlow(CreateGoalUiState(isEditMode = isEditMode))
    val uiState: StateFlow<CreateGoalUiState> = _uiState.asStateFlow()

    private var existingBankDeposit: BankDeposit? = null

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories().first()

            if (isEditMode) {
                val goal = goalRepository.getGoalById(goalId).first()
                val goalCategories = goalCategoryRepository.getCategoriesForGoal(goalId).first()
                val bankDeposit = bankDepositRepository.getByGoalId(goalId).first()
                existingBankDeposit = bankDeposit

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allCategories = categories,
                    name = goal?.name ?: "",
                    description = goal?.description ?: "",
                    targetAmount = goal?.targetAmount?.toString() ?: "",
                    deadline = goal?.deadline,
                    selectedCategoryIds = goalCategories.map { it.id }.toSet(),
                    depositEnabled = bankDeposit != null,
                    interestRate = bankDeposit?.interestRate?.toString() ?: "",
                    capitalizationMonths = bankDeposit?.capitalizationMonths ?: 1,
                    initialAmount = bankDeposit?.initialAmount?.toString() ?: "",
                    openingDate = bankDeposit?.startDate
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, allCategories = categories)
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errors = _uiState.value.errors.copy(name = null))
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onTargetAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(targetAmount = value, errors = _uiState.value.errors.copy(targetAmount = null))
    }

    /** Дедлайн необязателен: null — приложение будет считать дату само (см. DeadlineForecast). */
    fun onDeadlineChange(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(deadline = date)
    }

    fun toggleCategory(categoryId: Int) {
        val current = _uiState.value.selectedCategoryIds
        _uiState.value = _uiState.value.copy(
            selectedCategoryIds = if (categoryId in current) current - categoryId else current + categoryId
        )
    }

    fun onDepositToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(depositEnabled = enabled)
    }

    fun onInterestRateChange(value: String) {
        _uiState.value = _uiState.value.copy(interestRate = value, errors = _uiState.value.errors.copy(interestRate = null))
    }

    fun onCapitalizationChange(months: Int) {
        _uiState.value = _uiState.value.copy(capitalizationMonths = months)
    }

    fun onInitialAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(initialAmount = value, errors = _uiState.value.errors.copy(initialAmount = null))
    }

    fun onOpeningDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(openingDate = date)
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    // --- Добавление пользовательской категории (простой диалог) ---

    fun openAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            isAddCategoryDialogOpen = true,
            newCategoryName = "",
            newCategoryColorHex = CategoryPalette.toHex(CategoryPalette.PRESET_COLORS.first())
        )
    }

    fun dismissAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(isAddCategoryDialogOpen = false)
    }

    fun onNewCategoryNameChange(value: String) {
        _uiState.value = _uiState.value.copy(newCategoryName = value)
    }

    fun onNewCategoryColorSelected(hex: String) {
        _uiState.value = _uiState.value.copy(newCategoryColorHex = hex)
    }

    fun confirmAddCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val newId = categoryRepository.insert(
                Category(name = name, icon = "●", color = _uiState.value.newCategoryColorHex)
            ).toInt()
            val refreshed = categoryRepository.getAllCategories().first()
            _uiState.value = _uiState.value.copy(
                allCategories = refreshed,
                selectedCategoryIds = _uiState.value.selectedCategoryIds + newId,
                isAddCategoryDialogOpen = false
            )
        }
    }

    // --- Сохранение цели ---

    fun save() {
        val state = _uiState.value

        val targetAmountValue = state.targetAmount.toDoubleOrNull()
        val interestRateValue = state.interestRate.toDoubleOrNull()
        val initialAmountValue = state.initialAmount.toDoubleOrNull()

        val errors = CreateGoalErrors(
            name = if (state.name.isBlank()) "Название цели обязательно" else null,
            targetAmount = when {
                targetAmountValue == null -> "Введите корректную сумму"
                targetAmountValue <= 0 -> "Введите корректную сумму"
                else -> null
            },
            // Дедлайн больше не обязателен — не задан, значит будет рассчитан автоматически.
            interestRate = if (!state.depositEnabled) null else when {
                interestRateValue == null -> "Введите корректную ставку"
                interestRateValue <= 0 -> "Введите корректную ставку"
                else -> null
            },
            initialAmount = if (!state.depositEnabled) null else when {
                initialAmountValue == null -> "Введите корректную сумму"
                initialAmountValue < 0 -> "Введите корректную сумму"
                else -> null
            }
        )

        val hasErrors = listOfNotNull(
            errors.name, errors.targetAmount, errors.interestRate, errors.initialAmount
        ).isNotEmpty()

        if (hasErrors) {
            _uiState.value = state.copy(errors = errors, snackbarMessage = "Пожалуйста, исправьте ошибки выше")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val savedGoalId: Int = if (isEditMode) {
                val existingGoal = goalRepository.getGoalById(goalId).first()
                val updated = (existingGoal ?: Goal(
                    id = goalId,
                    name = "",
                    description = "",
                    targetAmount = 0.0,
                    deadline = state.deadline,
                    createdDate = LocalDate.now()
                )).copy(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    targetAmount = targetAmountValue!!,
                    deadline = state.deadline
                )
                goalRepository.update(updated)
                goalId
            } else {
                val newGoal = Goal(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    targetAmount = targetAmountValue!!,
                    deadline = state.deadline,
                    createdDate = LocalDate.now()
                )
                goalRepository.insert(newGoal).toInt()
            }

            // Категории M:N — пересобираем связи целиком. Если ничего не выбрано — привязываем "Other".
            goalCategoryRepository.deleteAllForGoal(savedGoalId)
            val categoryIdsToLink = state.selectedCategoryIds.ifEmpty {
                state.allCategories.firstOrNull { it.name == "Other" }
                    ?.let { setOf(it.id) }
                    ?: emptySet()
            }
            categoryIdsToLink.forEach { categoryId ->
                goalCategoryRepository.insert(GoalCategory(goalId = savedGoalId, categoryId = categoryId))
            }

            // Банковский вклад — создать/обновить/удалить в зависимости от переключателя.
            if (state.depositEnabled) {
                val deposit = BankDeposit(
                    id = existingBankDeposit?.id ?: 0,
                    goalId = savedGoalId,
                    interestRate = interestRateValue!!,
                    capitalizationMonths = state.capitalizationMonths,
                    startDate = state.openingDate ?: LocalDate.now(),
                    initialAmount = initialAmountValue ?: 0.0
                )
                if (existingBankDeposit != null) {
                    bankDepositRepository.update(deposit)
                } else {
                    bankDepositRepository.insert(deposit)
                }
            } else {
                existingBankDeposit?.let { bankDepositRepository.delete(it) }
            }

            _uiState.value = _uiState.value.copy(isSaving = false, saveCompleted = true)
        }
    }
}
