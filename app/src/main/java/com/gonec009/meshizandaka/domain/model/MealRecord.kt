package com.gonec009.meshizandaka.domain.model

data class MealRecord(
    val id: Long = 0,
    val eatenAt: Long,
    val mealType: MealType,
    val templateId: Long?,
    val templateNameSnapshot: String,
    val totalCalories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val isSpecial: Boolean,
    val specialDeltaCalories: Int,
    val sourceType: SourceType,
    val memo: String,
    val photoUri: String? = null,
    val selectedOptions: List<MealRecordOption> = emptyList(),
    val editedOptionGroupNames: Set<String> = emptySet(),
    val excludedDrivePlanItemKeys: Set<String> = emptySet(),
    val selectedDrivePlanMainDishItemKey: String? = null,
)

data class MealRecordOption(
    val id: Long = 0,
    val mealRecordId: Long = 0,
    val optionGroupNameSnapshot: String,
    val optionNameSnapshot: String,
    val calorieDelta: Int,
    val proteinDeltaG: Double,
    val fatDeltaG: Double,
    val carbDeltaG: Double,
)
