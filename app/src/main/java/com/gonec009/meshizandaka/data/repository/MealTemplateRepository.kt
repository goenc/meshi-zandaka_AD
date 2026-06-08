package com.gonec009.meshizandaka.data.repository

import com.gonec009.meshizandaka.data.local.dao.MealTemplateDao
import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.MealTemplateWithRelations
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SelectionType
import com.gonec009.meshizandaka.domain.model.TemplateOption
import com.gonec009.meshizandaka.domain.model.TemplateOptionGroup
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MealTemplateRepository(private val dao: MealTemplateDao) {
    fun observeActiveTemplates(): Flow<List<MealTemplate>> =
        dao.observeActiveTemplates().map { items -> items.map(::toModel) }

    fun observeNormalTemplates(): Flow<List<MealTemplate>> =
        dao.observeNormalTemplates().map { items ->
            items.map { entity ->
                MealTemplate(
                    id = entity.id,
                    name = entity.name,
                    mealType = MealType.valueOf(entity.mealType),
                    shortcutRole = TemplateShortcutRole.valueOf(entity.shortcutRole),
                    baseCalories = entity.baseCalories,
                    proteinG = entity.proteinG,
                    fatG = entity.fatG,
                    carbG = entity.carbG,
                    isSpecial = entity.isSpecial,
                    comparisonTemplateId = entity.comparisonTemplateId,
                    weeklyLimitCount = entity.weeklyLimitCount,
                    monthlyLimitCount = entity.monthlyLimitCount,
                    memo = entity.memo,
                    isActive = entity.isActive,
                )
            }
        }

    suspend fun getTemplate(templateId: Long): MealTemplate? = dao.getTemplate(templateId)?.let(::toModel)

    suspend fun countTemplates(): Int = dao.countTemplates()

    suspend fun insertSeedData(
        templates: List<MealTemplateEntity>,
        groupsByTemplateIndex: Map<Int, List<TemplateOptionGroupEntity>>,
        optionsByGroupIndex: Map<Pair<Int, Int>, List<TemplateOptionEntity>>,
    ): List<Long> {
        val insertedIds = dao.insertTemplates(templates)
        groupsByTemplateIndex.forEach { (templateIndex, groups) ->
            val insertedGroupIds = dao.insertOptionGroups(
                groups.map { it.copy(templateId = insertedIds[templateIndex]) },
            )
            groups.forEachIndexed { groupIndex, _ ->
                val key = templateIndex to groupIndex
                val options = optionsByGroupIndex[key].orEmpty()
                dao.insertOptions(options.map { it.copy(groupId = insertedGroupIds[groupIndex]) })
            }
        }
        return insertedIds
    }

    suspend fun saveTemplate(template: MealTemplate) {
        val entity = MealTemplateEntity(
            id = template.id,
            name = template.name,
            mealType = template.mealType.name,
            shortcutRole = template.shortcutRole.name,
            baseCalories = template.baseCalories,
            proteinG = template.proteinG,
            fatG = template.fatG,
            carbG = template.carbG,
            isSpecial = template.isSpecial,
            comparisonTemplateId = template.comparisonTemplateId,
            weeklyLimitCount = template.weeklyLimitCount,
            monthlyLimitCount = template.monthlyLimitCount,
            memo = template.memo,
            isActive = template.isActive,
        )
        if (template.id == 0L) {
            dao.insertTemplate(entity)
        } else {
            dao.updateTemplate(entity)
        }
    }

    private fun toModel(item: MealTemplateWithRelations): MealTemplate {
        return MealTemplate(
            id = item.template.id,
            name = item.template.name,
            mealType = MealType.valueOf(item.template.mealType),
            shortcutRole = TemplateShortcutRole.valueOf(item.template.shortcutRole),
            baseCalories = item.template.baseCalories,
            proteinG = item.template.proteinG,
            fatG = item.template.fatG,
            carbG = item.template.carbG,
            isSpecial = item.template.isSpecial,
            comparisonTemplateId = item.template.comparisonTemplateId,
            weeklyLimitCount = item.template.weeklyLimitCount,
            monthlyLimitCount = item.template.monthlyLimitCount,
            memo = item.template.memo,
            isActive = item.template.isActive,
            optionGroups = item.optionGroups.map { group ->
                TemplateOptionGroup(
                    id = group.group.id,
                    templateId = group.group.templateId,
                    name = group.group.name,
                    selectionType = SelectionType.valueOf(group.group.selectionType),
                    options = group.options.sortedBy { option -> option.sortOrder }.map { option ->
                        TemplateOption(
                            id = option.id,
                            groupId = option.groupId,
                            name = option.name,
                            calorieDelta = option.calorieDelta,
                            proteinDeltaG = option.proteinDeltaG,
                            fatDeltaG = option.fatDeltaG,
                            carbDeltaG = option.carbDeltaG,
                            sortOrder = option.sortOrder,
                        )
                    },
                )
            },
        )
    }
}
