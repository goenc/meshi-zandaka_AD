package com.gonec009.meshizandaka.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateEditorState(
    val id: Long = 0,
    val name: String = "",
    val mealType: MealType = MealType.LUNCH,
    val shortcutRole: TemplateShortcutRole = TemplateShortcutRole.NONE,
    val baseCalories: String = "",
    val proteinG: String = "",
    val fatG: String = "",
    val carbG: String = "",
    val isSpecial: Boolean = false,
    val comparisonTemplateId: Long? = null,
    val weeklyLimitCount: String = "",
    val monthlyLimitCount: String = "",
    val memo: String = "",
)

data class TemplateManagementUiState(
    val templates: List<MealTemplate> = emptyList(),
    val normalTemplates: List<MealTemplate> = emptyList(),
    val editorState: TemplateEditorState = TemplateEditorState(),
    val isDialogOpen: Boolean = false,
)

class TemplateManagementViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TemplateManagementUiState())
    val uiState: StateFlow<TemplateManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.mealTemplateRepository.observeActiveTemplates(),
                container.mealTemplateRepository.observeNormalTemplates(),
            ) { templates, normalTemplates ->
                templates to normalTemplates
            }.collect { (templates, normalTemplates) ->
                _uiState.update { it.copy(templates = templates, normalTemplates = normalTemplates) }
            }
        }
    }

    fun openNewDialog() {
        _uiState.update { it.copy(isDialogOpen = true, editorState = TemplateEditorState()) }
    }

    fun openEditDialog(template: MealTemplate) {
        _uiState.update {
            it.copy(
                isDialogOpen = true,
                editorState = TemplateEditorState(
                    id = template.id,
                    name = template.name,
                    mealType = template.mealType,
                    shortcutRole = template.shortcutRole,
                    baseCalories = template.baseCalories.toString(),
                    proteinG = template.proteinG.toString(),
                    fatG = template.fatG.toString(),
                    carbG = template.carbG.toString(),
                    isSpecial = template.isSpecial,
                    comparisonTemplateId = template.comparisonTemplateId,
                    weeklyLimitCount = template.weeklyLimitCount?.toString().orEmpty(),
                    monthlyLimitCount = template.monthlyLimitCount?.toString().orEmpty(),
                    memo = template.memo,
                ),
            )
        }
    }

    fun closeDialog() {
        _uiState.update { it.copy(isDialogOpen = false) }
    }

    fun updateEditor(transform: (TemplateEditorState) -> TemplateEditorState) {
        _uiState.update { it.copy(editorState = transform(it.editorState)) }
    }

    fun saveTemplate() {
        val editor = _uiState.value.editorState
        viewModelScope.launch {
            container.mealTemplateRepository.saveTemplate(
                MealTemplate(
                    id = editor.id,
                    name = editor.name.ifBlank { "新しいテンプレート" },
                    mealType = editor.mealType,
                    shortcutRole = editor.shortcutRole,
                    baseCalories = editor.baseCalories.toIntOrNull() ?: 0,
                    proteinG = editor.proteinG.toIntOrNull() ?: 0,
                    fatG = editor.fatG.toIntOrNull() ?: 0,
                    carbG = editor.carbG.toIntOrNull() ?: 0,
                    isSpecial = editor.isSpecial,
                    comparisonTemplateId = editor.comparisonTemplateId,
                    weeklyLimitCount = editor.weeklyLimitCount.toIntOrNull(),
                    monthlyLimitCount = editor.monthlyLimitCount.toIntOrNull(),
                    memo = editor.memo,
                ),
            )
            _uiState.update { it.copy(isDialogOpen = false, editorState = TemplateEditorState()) }
        }
    }
}
