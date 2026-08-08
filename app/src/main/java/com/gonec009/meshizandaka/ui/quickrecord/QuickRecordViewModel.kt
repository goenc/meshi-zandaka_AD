package com.gonec009.meshizandaka.ui.quickrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import com.gonec009.meshizandaka.domain.usecase.DuplicateDailyMealException
import com.gonec009.meshizandaka.domain.usecase.RecordAppendTargetException
import com.gonec009.meshizandaka.util.TimeRangeUtils
import com.gonec009.meshizandaka.util.sanitizeDecimalInput
import com.gonec009.meshizandaka.util.formatOneDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

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
)

data class QuickRecordUiState(
    val availableTemplates: List<MealTemplate> = emptyList(),
    val selectedTemplate: MealTemplate? = null,
    val driveEatingOutCards: List<QuickRecordDriveCard> = emptyList(),
    val selectedDriveEatingOutCard: QuickRecordDriveCard? = null,
    val templateName: String = "",
    val selectedMealType: MealType = MealType.LUNCH,
    val isSpecial: Boolean = false,
    val totalCalories: String = "",
    val proteinG: String = "",
    val fatG: String = "",
    val carbG: String = "",
    val memo: String = "",
    val photoUri: String? = null,
    val todayMealRecords: List<MealRecord> = emptyList(),
    val appendToRecordId: Long? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
)

class QuickRecordViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickRecordUiState())
    val uiState: StateFlow<QuickRecordUiState> = _uiState.asStateFlow()
    private val zoneId = ZoneId.systemDefault()

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
                    )
                }
                _uiState.update { state ->
                    val selectedCard = state.selectedDriveEatingOutCard?.let { current ->
                        cards.firstOrNull { card -> card.id == current.id }
                    }
                    state.copy(
                        driveEatingOutCards = cards,
                        selectedDriveEatingOutCard = selectedCard,
                    )
                }
            }
        }
        viewModelScope.launch {
            val (startInclusive, endInclusive) = TimeRangeUtils.todayRange(System.currentTimeMillis(), zoneId)
            container.mealRecordRepository.observeRecordsBetween(startInclusive, endInclusive).collect { records ->
                _uiState.update { state ->
                    state.copy(
                        todayMealRecords = records,
                        appendToRecordId = resolveAppendTarget(
                            selectedMealType = state.selectedMealType,
                            currentTargetId = state.appendToRecordId,
                            records = records,
                        ),
                    )
                }
            }
        }
    }

    fun selectTemplate(template: MealTemplate) {
        _uiState.update { state ->
            applyTemplate(state, template).copy(
                selectedDriveEatingOutCard = null,
                appendToRecordId = if (template.mealType == MealType.EATING_OUT) {
                    defaultAppendTarget(state.todayMealRecords)
                } else {
                    null
                },
            )
        }
    }

    fun selectDriveEatingOutCard(card: QuickRecordDriveCard) {
        _uiState.update { state ->
            state.copy(
                selectedTemplate = null,
                selectedDriveEatingOutCard = card,
                templateName = card.name,
                selectedMealType = MealType.EATING_OUT,
                isSpecial = true,
                totalCalories = card.calories.toString(),
                proteinG = formatOneDecimal(card.proteinG),
                fatG = formatOneDecimal(card.fatG),
                carbG = formatOneDecimal(card.carbG),
                memo = card.amountLabel,
                photoUri = card.imagePath,
                appendToRecordId = defaultAppendTarget(state.todayMealRecords),
            )
        }
    }

    fun selectMealType(mealType: MealType) {
        _uiState.update { state ->
            state.copy(
                selectedMealType = mealType,
                appendToRecordId = if (mealType == MealType.EATING_OUT) {
                    defaultAppendTarget(state.todayMealRecords)
                } else {
                    null
                },
            )
        }
    }

    fun selectAppendTarget(recordId: Long?) {
        _uiState.update { it.copy(appendToRecordId = recordId) }
    }

    fun setTemplateName(value: String) {
        _uiState.update { it.copy(templateName = value) }
    }

    fun setSpecial(isSpecial: Boolean) {
        _uiState.update { it.copy(isSpecial = isSpecial) }
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

    fun setMemo(value: String) {
        _uiState.update { it.copy(memo = value) }
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
        val template = state.selectedTemplate
        val driveCard = state.selectedDriveEatingOutCard
        if (template == null && driveCard == null) return
        val appendToRecordId = state.appendToRecordId.takeIf { state.selectedMealType == MealType.EATING_OUT }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                container.createQuickRecordUseCase(
                    templateId = template?.id,
                    templateNameSnapshot = state.templateName.ifBlank {
                        template?.name ?: driveCard?.name.orEmpty()
                    },
                    totalCaloriesOverride = state.totalCalories.toIntOrNull() ?: 0,
                    proteinOverride = state.proteinG.toDoubleOrNull() ?: 0.0,
                    fatOverride = state.fatG.toDoubleOrNull() ?: 0.0,
                    carbOverride = state.carbG.toDoubleOrNull() ?: 0.0,
                    isSpecialOverride = state.isSpecial,
                    memo = state.memo,
                    photoUri = state.photoUri,
                    mealType = state.selectedMealType,
                    appendToRecordId = appendToRecordId,
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        memo = "",
                        photoUri = null,
                        message = appendToRecordId?.let { recordId ->
                            it.todayMealRecords.firstOrNull { record -> record.id == recordId }
                                ?.let { record -> "${mealTypeName(record.mealType)}に追加しました" }
                        } ?: "記録しました",
                    )
                }
            } catch (error: DuplicateDailyMealException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message,
                    )
                }
            } catch (error: RecordAppendTargetException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message,
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
            isSpecial = template.isSpecial,
            totalCalories = template.baseCalories.toString(),
            proteinG = formatOneDecimal(template.proteinG),
            fatG = formatOneDecimal(template.fatG),
            carbG = formatOneDecimal(template.carbG),
            memo = template.memo,
            photoUri = template.photoUri,
        )
    }

    private fun defaultAppendTarget(records: List<MealRecord>): Long? {
        return records.filter { record ->
            record.mealType == MealType.LUNCH || record.mealType == MealType.DINNER
        }.singleOrNull()?.id
    }

    private fun resolveAppendTarget(
        selectedMealType: MealType,
        currentTargetId: Long?,
        records: List<MealRecord>,
    ): Long? {
        if (selectedMealType != MealType.EATING_OUT) return null
        val target = currentTargetId?.let { id -> records.firstOrNull { it.id == id } }
        return target?.id ?: defaultAppendTarget(records)
    }

    private fun mealTypeName(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> "朝食"
        MealType.MORNING_SNACK -> "間朝"
        MealType.LUNCH -> "昼食"
        MealType.DINNER -> "夕食"
        MealType.DAYTIME_SNACK -> "間昼"
        MealType.FREE_SNACK,
        MealType.SNACK,
        -> "間全"
        MealType.EATING_OUT -> "外食"
    }
}
