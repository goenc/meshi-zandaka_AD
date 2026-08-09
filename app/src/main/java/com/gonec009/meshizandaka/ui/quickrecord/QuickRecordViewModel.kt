package com.gonec009.meshizandaka.ui.quickrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import com.gonec009.meshizandaka.util.sanitizeDecimalInput
import com.gonec009.meshizandaka.util.formatOneDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickRecordDriveCard(
    val id: String,
    val mealLabel: String,
    val name: String,
    val amountLabel: String,
    val imagePath: String?,
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val items: List<QuickRecordDriveItem> = emptyList(),
)

data class QuickRecordDriveItem(
    val id: String,
    val name: String,
    val amountLabel: String,
    val imagePath: String? = null,
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
)

data class QuickRecordFood(
    val id: String,
    val name: String,
    val mealCategory: Int,
    val amountLabel: String,
    val imagePath: String?,
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
)

data class QuickRecordUiState(
    val availableTemplates: List<MealTemplate> = emptyList(),
    val selectedTemplate: MealTemplate? = null,
    val driveEatingOutCards: List<QuickRecordDriveCard> = emptyList(),
    val selectedDriveEatingOutCard: QuickRecordDriveCard? = null,
    val foods: List<QuickRecordFood> = emptyList(),
    val templateName: String = "",
    val selectedMealType: MealType = MealType.LUNCH,
    val totalCalories: String = "",
    val proteinG: String = "",
    val fatG: String = "",
    val carbG: String = "",
    val photoUri: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
)

class QuickRecordViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickRecordUiState())
    val uiState: StateFlow<QuickRecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.mealTemplateRepository.observeActiveTemplates().collect { templates ->
                val availableTemplates = templates.filter { template ->
                    template.shortcutRole == TemplateShortcutRole.NONE &&
                        template.mealType != MealType.EATING_OUT
                }
                val currentSelection = _uiState.value.selectedTemplate
                val currentDriveCard = _uiState.value.selectedDriveEatingOutCard
                val selectedTemplate = currentSelection?.let { selected ->
                    availableTemplates.firstOrNull { it.id == selected.id }
                } ?: defaultTemplate(availableTemplates)
                _uiState.update {
                    when {
                        currentDriveCard != null && currentSelection == null -> it.copy(
                            availableTemplates = availableTemplates,
                        )
                        selectedTemplate == null -> it.copy(
                            availableTemplates = availableTemplates,
                            selectedTemplate = null,
                        )
                        currentSelection == null -> applyTemplate(
                            state = it.copy(availableTemplates = availableTemplates),
                            template = selectedTemplate,
                        )
                        currentSelection.id != selectedTemplate.id -> applyTemplate(
                            state = it.copy(availableTemplates = availableTemplates),
                            template = selectedTemplate,
                        )
                        else -> it.copy(
                            availableTemplates = availableTemplates,
                            selectedTemplate = selectedTemplate,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            container.driveAccessManager.planState.collect { planState ->
                val cards = planState.externalCards.map { card ->
                    QuickRecordDriveCard(
                        id = card.id,
                        mealLabel = listOfNotNull(card.storeName, card.tabName)
                            .joinToString(" / ")
                            .ifBlank { "Driveの外食カード" },
                        name = card.name,
                        amountLabel = card.amountLabel,
                        imagePath = card.imagePath,
                        calories = card.calories,
                        proteinG = card.proteinG,
                        fatG = card.fatG,
                        carbG = card.carbG,
                        items = card.items.map { item ->
                            QuickRecordDriveItem(
                                id = item.id ?: "${card.id}:${item.name}",
                                name = item.name,
                                amountLabel = item.amountLabel,
                                imagePath = item.imagePath,
                                calories = item.calories,
                                proteinG = item.proteinG,
                                fatG = item.fatG,
                                carbG = item.carbG,
                            )
                        },
                    )
                }
                val foods = planState.foods.map { food ->
                    QuickRecordFood(
                        id = food.id,
                        name = food.name,
                        mealCategory = food.mealCategory,
                        amountLabel = food.amountLabel,
                        imagePath = food.imagePath,
                        calories = food.calories,
                        proteinG = food.proteinG,
                        fatG = food.fatG,
                        carbG = food.carbG,
                    )
                }
                _uiState.update { state ->
                    val selectedCard = state.selectedDriveEatingOutCard?.let { current ->
                        cards.firstOrNull { card -> card.id == current.id }
                    }
                    state.copy(
                        driveEatingOutCards = cards,
                        selectedDriveEatingOutCard = selectedCard,
                        foods = foods,
                    )
                }
            }
        }
    }

    fun selectTemplate(template: MealTemplate) {
        _uiState.update { state -> applyTemplate(state, template).copy(selectedDriveEatingOutCard = null) }
    }

    fun selectDriveEatingOutCard(card: QuickRecordDriveCard) {
        _uiState.update { state ->
            state.copy(
                selectedTemplate = null,
                selectedDriveEatingOutCard = card,
                templateName = card.name,
                selectedMealType = state.selectedMealType.takeUnless { it == MealType.EATING_OUT }
                    ?: MealType.LUNCH,
                totalCalories = card.calories.toString(),
                proteinG = formatOneDecimal(card.proteinG),
                fatG = formatOneDecimal(card.fatG),
                carbG = formatOneDecimal(card.carbG),
                photoUri = null,
            )
        }
    }

    fun selectManualTab() {
        _uiState.update { state ->
            val clearedState = state.copy(selectedDriveEatingOutCard = null)
            if (clearedState.selectedTemplate != null) {
                clearedState
            } else {
                defaultTemplate(clearedState.availableTemplates)?.let { template ->
                    applyTemplate(clearedState, template)
                } ?: clearedState
            }
        }
    }

    fun selectFoodTab() {
        _uiState.update { it.copy(selectedDriveEatingOutCard = null) }
    }

    fun selectMealType(mealType: MealType) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun setTemplateName(value: String) {
        _uiState.update { it.copy(templateName = value) }
    }

    fun setTotalCalories(value: String) {
        _uiState.update { it.copy(totalCalories = value) }
    }

    fun setProtein(value: String) {
        _uiState.update { it.copy(proteinG = sanitizeDecimalInput(value)) }
    }

    fun setFat(value: String) {
        _uiState.update { it.copy(fatG = sanitizeDecimalInput(value)) }
    }

    fun setCarb(value: String) {
        _uiState.update { it.copy(carbG = sanitizeDecimalInput(value)) }
    }

    fun setPhotoUri(photoUri: String?) {
        _uiState.update {
            it.copy(
                photoUri = photoUri,
                message = if (photoUri == null) "写真の撮影をキャンセルしました" else "写真を追加しました",
            )
        }
    }

    fun clearPhoto() {
        _uiState.update {
            it.copy(
                photoUri = null,
                message = "写真を削除しました",
            )
        }
    }

    fun saveRecord() {
        val state = _uiState.value
        val driveCard = state.selectedDriveEatingOutCard
        val template = state.templateForSave()
        if (!state.canSave()) return
        val mealType = state.recordableMealType()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val additionalOptions = driveCard?.toRecordOptions().orEmpty()
            container.createQuickRecordUseCase(
                templateId = template?.id,
                templateNameSnapshot = state.templateName.ifBlank {
                    template?.name ?: driveCard?.name.orEmpty()
                },
                additionalOptions = additionalOptions,
                totalCaloriesOverride = state.totalCalories.toIntOrNull() ?: 0,
                proteinOverride = state.proteinG.toDoubleOrNull() ?: 0.0,
                fatOverride = state.fatG.toDoubleOrNull() ?: 0.0,
                carbOverride = state.carbG.toDoubleOrNull() ?: 0.0,
                isSpecialOverride = false,
                memo = "",
                photoUri = state.photoUri,
                mealType = mealType,
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    photoUri = null,
                    message = "記録しました",
                )
            }
        }
    }

    fun registerFood(food: QuickRecordFood) {
        val state = _uiState.value
        val mealType = state.selectedMealType.takeUnless {
            it == MealType.EATING_OUT || it == MealType.SNACK
        } ?: MealType.LUNCH
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                container.createQuickRecordUseCase(
                    templateId = null,
                    templateNameSnapshot = food.name,
                    additionalOptions = listOf(food.toRecordOption()),
                    totalCaloriesOverride = food.calories,
                    proteinOverride = food.proteinG,
                    fatOverride = food.fatG,
                    carbOverride = food.carbG,
                    isSpecialOverride = false,
                    mealType = mealType,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "記録しました",
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = exception.message ?: "記録に失敗しました",
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun defaultTemplate(availableTemplates: List<MealTemplate>): MealTemplate? {
        return availableTemplates.firstOrNull()
    }

    private fun applyTemplate(state: QuickRecordUiState, template: MealTemplate): QuickRecordUiState {
        return state.copy(
            selectedTemplate = template,
            templateName = template.name,
            selectedMealType = template.mealType,
            totalCalories = template.baseCalories.toString(),
            proteinG = formatOneDecimal(template.proteinG),
            fatG = formatOneDecimal(template.fatG),
            carbG = formatOneDecimal(template.carbG),
            photoUri = template.photoUri,
        )
    }
}

internal fun QuickRecordUiState.templateForSave(): MealTemplate? {
    return selectedTemplate.takeUnless { selectedDriveEatingOutCard != null }
}

internal fun QuickRecordUiState.recordableMealType(): MealType {
    return selectedMealType.takeUnless {
        it == MealType.EATING_OUT || it == MealType.SNACK
    } ?: MealType.LUNCH
}

internal fun QuickRecordUiState.canSave(): Boolean {
    return selectedDriveEatingOutCard != null ||
        templateForSave() != null ||
        templateName.isNotBlank()
}

internal fun QuickRecordDriveCard.toRecordOptions(): List<MealRecordOption> {
    return items.map { item ->
        MealRecordOption(
            optionGroupNameSnapshot = name,
            optionNameSnapshot = item.name,
            calorieDelta = item.calories,
            proteinDeltaG = item.proteinG,
            fatDeltaG = item.fatG,
            carbDeltaG = item.carbG,
        )
    }
}

private fun QuickRecordFood.toRecordOption(): MealRecordOption = MealRecordOption(
    optionGroupNameSnapshot = "食品",
    optionNameSnapshot = name,
    calorieDelta = calories,
    proteinDeltaG = proteinG,
    fatDeltaG = fatG,
    carbDeltaG = carbG,
)
