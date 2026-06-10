package com.gonec009.meshizandaka.ui.quickrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import com.gonec009.meshizandaka.domain.usecase.DuplicateDailyMealException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickRecordUiState(
    val availableTemplates: List<MealTemplate> = emptyList(),
    val selectedTemplate: MealTemplate? = null,
    val selectedMealType: MealType = MealType.LUNCH,
    val isSpecial: Boolean = false,
    val totalCalories: String = "",
    val proteinG: String = "",
    val fatG: String = "",
    val carbG: String = "",
    val memo: String = "",
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
                    template.shortcutRole == TemplateShortcutRole.NONE
                }
                val currentSelection = _uiState.value.selectedTemplate
                val selectedTemplate = currentSelection?.let { selected ->
                    availableTemplates.firstOrNull { it.id == selected.id }
                } ?: defaultTemplate(availableTemplates)
                _uiState.update {
                    when {
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
    }

    fun selectTemplate(template: MealTemplate) {
        _uiState.update { applyTemplate(it, template) }
    }

    fun selectMealType(mealType: MealType) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun setSpecial(isSpecial: Boolean) {
        _uiState.update { it.copy(isSpecial = isSpecial) }
    }

    fun setTotalCalories(value: String) {
        _uiState.update { it.copy(totalCalories = value) }
    }

    fun setProtein(value: String) {
        _uiState.update { it.copy(proteinG = value) }
    }

    fun setFat(value: String) {
        _uiState.update { it.copy(fatG = value) }
    }

    fun setCarb(value: String) {
        _uiState.update { it.copy(carbG = value) }
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

    fun saveRecord() {
        val template = _uiState.value.selectedTemplate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                container.createQuickRecordUseCase(
                    templateId = template.id,
                    templateNameSnapshot = template.name,
                    totalCaloriesOverride = _uiState.value.totalCalories.toIntOrNull() ?: 0,
                    proteinOverride = _uiState.value.proteinG.toIntOrNull() ?: 0,
                    fatOverride = _uiState.value.fatG.toIntOrNull() ?: 0,
                    carbOverride = _uiState.value.carbG.toIntOrNull() ?: 0,
                    isSpecialOverride = _uiState.value.isSpecial,
                    memo = _uiState.value.memo,
                    photoUri = _uiState.value.photoUri,
                    mealType = _uiState.value.selectedMealType,
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        memo = "",
                        photoUri = null,
                        message = "記録しました",
                    )
                }
            } catch (error: DuplicateDailyMealException) {
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
            selectedMealType = template.mealType,
            isSpecial = template.isSpecial,
            totalCalories = template.baseCalories.toString(),
            proteinG = template.proteinG.toString(),
            fatG = template.fatG.toString(),
            carbG = template.carbG.toString(),
            memo = template.memo,
            photoUri = template.photoUri,
        )
    }
}
