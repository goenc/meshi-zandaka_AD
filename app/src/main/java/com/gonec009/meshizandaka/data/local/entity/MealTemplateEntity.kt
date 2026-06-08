package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mealType: String,
    val baseCalories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int,
    val isSpecial: Boolean,
    val comparisonTemplateId: Long?,
    val weeklyLimitCount: Int?,
    val monthlyLimitCount: Int?,
    val memo: String,
    val isActive: Boolean = true,
)
