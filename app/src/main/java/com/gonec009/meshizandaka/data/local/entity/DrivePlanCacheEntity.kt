package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drive_plan_cache")
data class DrivePlanCacheEntity(
    @PrimaryKey val datasetId: String,
    val catalogJson: String,
    val selectedPlanId: String?,
    val updatedAtMillis: Long,
)
