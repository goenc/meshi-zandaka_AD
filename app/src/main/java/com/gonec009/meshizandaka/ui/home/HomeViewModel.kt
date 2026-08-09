package com.gonec009.meshizandaka.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.recordKey
import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.DashboardSummary
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import com.gonec009.meshizandaka.domain.model.WeeklyMealChart
import com.gonec009.meshizandaka.domain.usecase.DuplicateDailyMealException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class HomeUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val weeklyChart: WeeklyMealChart = WeeklyMealChart(),
    val settings: AppSettings = AppSettings(),
    val templates: List<com.gonec009.meshizandaka.domain.model.MealTemplate> = emptyList(),
    val selectedRecordDate: LocalDate = LocalDate.now(),
    val message: String? = null,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settingsFlow,
                container.mealTemplateRepository.observeActiveTemplates(),
                container.observeDashboardUseCase(),
            ) { settings, templates, dashboard -> Triple(settings, templates, dashboard) }
                .collect { (settings, templates, dashboard) ->
                    _uiState.update {
                        it.copy(
                            summary = dashboard.summary,
                            weeklyChart = dashboard.weeklyChart,
                            settings = settings,
                            templates = templates,
                        )
                    }
                }
        }
    }

    fun recordBreakfast() {
        recordTemplate(TemplateShortcutRole.BREAKFAST, "朝セットを記録しました")
    }

    fun recordMorningSnack() {
        recordTemplate(TemplateShortcutRole.MORNING_SNACK, "間朝セットを記録しました")
    }

    fun recordLunch() {
        recordTemplate(TemplateShortcutRole.LUNCH, "昼セットを記録しました")
    }

    fun recordDinner() {
        recordTemplate(TemplateShortcutRole.DINNER, "夕セットを記録しました")
    }

    fun recordDaytimeSnack() {
        recordTemplate(TemplateShortcutRole.DAYTIME_SNACK, "間昼セットを記録しました")
    }

    fun recordFreeSnack() {
        recordTemplate(TemplateShortcutRole.FREE_SNACK, "間全セットを記録しました")
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun moveSelectedRecordDate(days: Long) {
        _uiState.update { it.copy(selectedRecordDate = it.selectedRecordDate.plusDays(days)) }
    }

    fun updateSelectedRecordDate(date: LocalDate) {
        _uiState.update { it.copy(selectedRecordDate = date) }
    }

    fun deleteRecord(recordId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            container.mealRecordRepository.deleteRecord(recordId)
            _uiState.update { it.copy(message = "記録を削除しました") }
            onComplete()
        }
    }

    fun deleteRecordPhoto(recordId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val record = container.mealRecordRepository.getRecord(recordId)
                ?.takeIf { !it.photoUri.isNullOrBlank() }
            if (record != null) {
                container.mealRecordRepository.updateRecord(record.copy(photoUri = null))
                _uiState.update { it.copy(message = "写真を削除しました") }
            }
            onComplete()
        }
    }

    fun deleteDrivePlanItem(
        recordId: Long,
        itemKey: String,
        item: DrivePlanItem,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            val deleted = container.mealRecordRepository.excludeDrivePlanItem(
                recordId = recordId,
                itemKey = itemKey,
                calories = item.calories,
                proteinG = item.proteinG,
                fatG = item.fatG,
                carbG = item.carbG,
            )
            if (deleted) {
                _uiState.update { it.copy(message = "${item.name}をこの日の記録から削除しました") }
            }
            onComplete()
        }
    }

    fun deleteRecordOption(
        recordId: Long,
        option: MealRecordOption,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            val deleted = container.mealRecordRepository.deleteRecordOption(recordId, option)
            if (deleted) {
                _uiState.update { it.copy(message = "${option.optionNameSnapshot}をこの日の記録から削除しました") }
            }
            onComplete()
        }
    }

    fun selectDrivePlanMainDish(
        recordId: Long,
        currentItem: DrivePlanItem?,
        selectedItem: DrivePlanItem,
        selectedItemKey: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            val changed = container.mealRecordRepository.selectDrivePlanMainDish(
                recordId = recordId,
                currentItem = currentItem,
                selectedItem = selectedItem,
                selectedItemKey = selectedItemKey,
            )
            if (changed) {
                _uiState.update { it.copy(message = "${selectedItem.name}を主菜に切り替えました") }
            }
            onComplete()
        }
    }

    private fun recordTemplate(role: TemplateShortcutRole, successMessage: String) {
        val templateId = resolveTemplateId(role)
        if (templateId == null) {
            _uiState.update { it.copy(message = "設定でテンプレートを選んでください") }
            return
        }
        val selectedDrivePlanMainDishItemKey = resolveDrivePlanMainDishItemKey(templateId, role)
        viewModelScope.launch {
            try {
                val zoneId = ZoneId.systemDefault()
                val selectedDate = _uiState.value.selectedRecordDate
                val recordMillis = selectedDate
                    .atTime(LocalTime.now(zoneId))
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
                container.createQuickRecordUseCase(
                    templateId = templateId,
                    selectedOptionIds = emptyList(),
                    isSetRegistration = true,
                    selectedDrivePlanMainDishItemKey = selectedDrivePlanMainDishItemKey,
                    nowMillis = recordMillis,
                    zoneId = zoneId,
                )
                _uiState.update {
                    it.copy(message = successMessage)
                }
            } catch (error: DuplicateDailyMealException) {
                _uiState.update {
                    it.copy(message = error.message)
                }
            }
        }
    }

    private fun resolveTemplateId(role: TemplateShortcutRole): Long? {
        return _uiState.value.templates.firstOrNull { it.shortcutRole == role }?.id
            ?: when (role) {
                TemplateShortcutRole.BREAKFAST -> _uiState.value.settings.defaultBreakfastTemplateId
                TemplateShortcutRole.LUNCH -> _uiState.value.settings.defaultLunchTemplateId
                TemplateShortcutRole.DINNER -> _uiState.value.settings.defaultDinnerTemplateId
                TemplateShortcutRole.MORNING_SNACK -> resolveNormalTemplateId(MealType.MORNING_SNACK)
                TemplateShortcutRole.DAYTIME_SNACK -> resolveNormalTemplateId(MealType.DAYTIME_SNACK)
                TemplateShortcutRole.FREE_SNACK -> resolveNormalTemplateId(MealType.FREE_SNACK, MealType.SNACK)
                TemplateShortcutRole.NONE -> null
            }
    }

    private fun resolveDrivePlanMainDishItemKey(
        templateId: Long,
        role: TemplateShortcutRole,
    ): String? {
        val template = _uiState.value.templates.firstOrNull { it.id == templateId }
        if (template?.shortcutRole != role) return null
        val slot = role.drivePlanSlot() ?: return null
        return container.driveAccessManager.planState.value.selectedPlan
            ?.meals
            ?.firstOrNull { it.slot == slot }
            ?.items
            ?.mapIndexed { index, item -> item to index }
            ?.firstOrNull { (item, _) -> item.isMainDish }
            ?.let { (item, index) -> item.recordKey(index) }
    }

    private fun TemplateShortcutRole.drivePlanSlot(): Int? = when (this) {
        TemplateShortcutRole.BREAKFAST -> 0
        TemplateShortcutRole.MORNING_SNACK -> 1
        TemplateShortcutRole.LUNCH -> 2
        TemplateShortcutRole.DINNER -> 3
        TemplateShortcutRole.DAYTIME_SNACK -> 4
        TemplateShortcutRole.FREE_SNACK -> 5
        TemplateShortcutRole.NONE -> null
    }

    private fun resolveNormalTemplateId(vararg mealTypes: MealType): Long? {
        return _uiState.value.templates.firstOrNull { template ->
            template.shortcutRole == TemplateShortcutRole.NONE && template.mealType in mealTypes
        }?.id
    }
}
