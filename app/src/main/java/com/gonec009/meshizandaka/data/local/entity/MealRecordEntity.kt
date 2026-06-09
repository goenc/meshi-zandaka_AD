package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_records")
data class MealRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eatenAt: Long,
    val mealType: String,
    val templateId: Long?,
    val templateNameSnapshot: String,
    val totalCalories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int,
    val isSpecial: Boolean,
    val specialDeltaCalories: Int,
    val sourceType: String,
    val memo: String,
    val photoUri: String? = null,
)
