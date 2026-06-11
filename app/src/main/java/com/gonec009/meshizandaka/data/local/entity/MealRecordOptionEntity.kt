package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_record_options",
    foreignKeys = [
        ForeignKey(
            entity = MealRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealRecordId")],
)
data class MealRecordOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealRecordId: Long,
    val optionGroupNameSnapshot: String,
    val optionNameSnapshot: String,
    val calorieDelta: Int,
    val proteinDeltaG: Double,
    val fatDeltaG: Double,
    val carbDeltaG: Double,
)
