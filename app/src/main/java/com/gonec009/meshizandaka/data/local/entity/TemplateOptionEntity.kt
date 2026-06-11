package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_options",
    foreignKeys = [
        ForeignKey(
            entity = TemplateOptionGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class TemplateOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val calorieDelta: Int,
    val proteinDeltaG: Double,
    val fatDeltaG: Double,
    val carbDeltaG: Double,
    val sortOrder: Int,
)
