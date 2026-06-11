package com.gonec009.meshizandaka.domain.model

data class MealTemplate(
    val id: Long = 0,
    val name: String,
    val mealType: MealType,
    val shortcutRole: TemplateShortcutRole = TemplateShortcutRole.NONE,
    val baseCalories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val isSpecial: Boolean,
    val comparisonTemplateId: Long?,
    val weeklyLimitCount: Int?,
    val monthlyLimitCount: Int?,
    val memo: String,
    val photoUri: String? = null,
    val isActive: Boolean = true,
    val optionGroups: List<TemplateOptionGroup> = emptyList(),
)

data class TemplateOptionGroup(
    val id: Long = 0,
    val templateId: Long = 0,
    val name: String,
    val selectionType: SelectionType,
    val options: List<TemplateOption> = emptyList(),
)

data class TemplateOption(
    val id: Long = 0,
    val groupId: Long = 0,
    val name: String,
    val calorieDelta: Int,
    val proteinDeltaG: Double,
    val fatDeltaG: Double,
    val carbDeltaG: Double,
    val sortOrder: Int,
)
