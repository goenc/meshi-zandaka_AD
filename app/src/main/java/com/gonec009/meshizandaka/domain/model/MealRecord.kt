package com.gonec009.meshizandaka.domain.model

data class MealRecord(
    val id: Long = 0,
    val eatenAt: Long,
    val mealType: MealType,
    val templateId: Long?,
    val templateNameSnapshot: String,
    val totalCalories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int,
    val isSpecial: Boolean,
    val specialDeltaCalories: Int,
    val sourceType: SourceType,
    val memo: String,
    val selectedOptions: List<MealRecordOption> = emptyList(),
)

data class MealRecordOption(
    val id: Long = 0,
    val mealRecordId: Long = 0,
    val optionGroupNameSnapshot: String,
    val optionNameSnapshot: String,
    val calorieDelta: Int,
    val proteinDeltaG: Int,
    val fatDeltaG: Int,
    val carbDeltaG: Int,
)
