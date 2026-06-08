package com.gonec009.meshizandaka.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MealTemplateWithRelations(
    @Embedded val template: MealTemplateEntity,
    @Relation(
        entity = TemplateOptionGroupEntity::class,
        parentColumn = "id",
        entityColumn = "templateId",
    )
    val optionGroups: List<TemplateOptionGroupWithOptions>,
)

data class TemplateOptionGroupWithOptions(
    @Embedded val group: TemplateOptionGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId",
    )
    val options: List<TemplateOptionEntity>,
)
