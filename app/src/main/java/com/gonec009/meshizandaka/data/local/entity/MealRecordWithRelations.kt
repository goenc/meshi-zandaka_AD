package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MealRecordWithRelations(
    @Embedded val record: MealRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealRecordId",
    )
    val selectedOptions: List<MealRecordOptionEntity>,
)
