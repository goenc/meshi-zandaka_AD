package com.gonec009.meshizandaka.ui.quickrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.usecase.DuplicateDailyMealException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickRecordUiState(
    val specialTemplates: List<MealTemplate> = emptyList(),
    val selectedTemplate: MealTemplate? = null,
    val selectedOptionIds: Map<Long, Long> = emptyMap(),
    val estimatedCalories: Int = 0,
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
                val specialTemplates = templates.filter { it.isSpecial }
                val currentSelection = _uiState.value.selectedTemplate
                val selectedTemplate = currentSelection?.let { selected ->
                    specialTemplates.firstOrNull { it.id == selected.id }
                } ?: defaultTemplate(specialTemplates)
                val selectedOptionIds = selectedTemplate?.let(::defaultOptionIds).orEmpty()
                _uiState.update {
                    it.copy(
                        specialTemplates = specialTemplates,
                        selectedTemplate = selectedTemplate,
                        selectedOptionIds = if (currentSelection == null) selectedOptionIds else it.selectedOptionIds.ifEmpty { selectedOptionIds },
                        estimatedCalories = selectedTemplate?.let { template ->
                            calculateCalories(template, if (currentSelection == null) selectedOptionIds else it.selectedOptionIds.ifEmpty { selectedOptionIds })
                        } ?: 0,
                    )
                }
            }
        }
    }

    fun selectTemplate(template: MealTemplate) {
        val optionIds = defaultOptionIds(template)
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                selectedOptionIds = optionIds,
                estimatedCalories = calculateCalories(template, optionIds),
            )
        }
    }

    fun selectOption(groupId: Long, optionId: Long) {
        _uiState.update { state ->
            val template = state.selectedTemplate ?: return@update state
            val selected = state.selectedOptionIds + (groupId to optionId)
            state.copy(
                selectedOptionIds = selected,
                estimatedCalories = calculateCalories(template, selected),
            )
        }
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
                    selectedOptionIds = _uiState.value.selectedOptionIds.values,
                    photoUri = _uiState.value.photoUri,
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
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

    private fun defaultTemplate(specialTemplates: List<MealTemplate>): MealTemplate? {
        return specialTemplates.firstOrNull()
    }

    private fun defaultOptionIds(template: MealTemplate): Map<Long, Long> {
        return template.optionGroups.mapNotNull { group ->
            group.options.firstOrNull()?.let { option -> group.id to option.id }
        }.toMap()
    }

    private fun calculateCalories(template: MealTemplate, selectedOptionIds: Map<Long, Long>): Int {
        return template.baseCalories + template.optionGroups.sumOf { group ->
            group.options.firstOrNull { option -> option.id == selectedOptionIds[group.id] }?.calorieDelta ?: 0
        }
    }
}
