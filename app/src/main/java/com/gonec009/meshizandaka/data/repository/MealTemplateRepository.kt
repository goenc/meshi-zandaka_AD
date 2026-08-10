package com.gonec009.meshizandaka.data.repository

import android.content.Context
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
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

class MealTemplateRepository(
    private val dao: MealTemplateDao,
    private val context: Context? = null,
) {
    private val photoPrefs = context?.getSharedPreferences(TEMPLATE_PHOTO_PREFS, Context.MODE_PRIVATE)

    fun observeActiveTemplates(): Flow<List<MealTemplate>> =
        dao.observeActiveTemplates().map { items -> items.map(::toModel) }

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

    suspend fun syncDriveShortcuts(plan: DrivePlan): Map<TemplateShortcutRole, Long> {
        val mappings = listOf(
            TemplateShortcutRole.BREAKFAST to plan.meals.firstOrNull { it.slot == 0 },
            TemplateShortcutRole.MORNING_SNACK to plan.meals.firstOrNull { it.slot == 1 },
            TemplateShortcutRole.LUNCH to plan.meals.firstOrNull { it.slot == 2 },
            TemplateShortcutRole.DINNER to plan.meals.firstOrNull { it.slot == 3 },
            TemplateShortcutRole.DAYTIME_SNACK to plan.meals.firstOrNull { it.slot == 4 },
            TemplateShortcutRole.FREE_SNACK to plan.meals.firstOrNull { it.slot == 5 },
        )
        val syncedIds = linkedMapOf<TemplateShortcutRole, Long>()
        mappings.forEach { (role, meal) ->
            if (meal == null) return@forEach
            if (!meal.nutritionDataAvailable) return@forEach
            val templateId = driveShortcutTemplateId(role)
            dao.deactivateActiveTemplatesByShortcutRole(role.name)
            dao.insertTemplate(
                MealTemplateEntity(
                    id = templateId,
                    name = meal.name.ifBlank { role.defaultTemplateName() },
                    mealType = role.mealType().name,
                    shortcutRole = role.name,
                    baseCalories = meal.totalCalories,
                    proteinG = meal.proteinG,
                    fatG = meal.fatG,
                    carbG = meal.carbG,
                    isSpecial = false,
                    comparisonTemplateId = null,
                    weeklyLimitCount = null,
                    monthlyLimitCount = null,
                    memo = buildDriveShortcutMemo(plan, meal),
                    isActive = true,
                ),
            )
            syncedIds[role] = templateId
        }
        return syncedIds
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
            photoUri = loadPhotoUri(item.template.id),
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

    private fun loadPhotoUri(templateId: Long): String? {
        if (templateId == 0L) return null
        return photoPrefs?.getString(templatePhotoKey(templateId), null)
    }

    private fun templatePhotoKey(templateId: Long): String = "template_photo_$templateId"

    private fun buildDriveShortcutMemo(plan: DrivePlan, meal: DrivePlanMeal): String =
        listOf("Windowsから同期", "プラン: ${plan.name}", meal.memo)
            .filter { it.isNotBlank() }
            .joinToString(" / ")

    private fun TemplateShortcutRole.mealType(): MealType = when (this) {
        TemplateShortcutRole.BREAKFAST -> MealType.BREAKFAST
        TemplateShortcutRole.MORNING_SNACK -> MealType.MORNING_SNACK
        TemplateShortcutRole.LUNCH -> MealType.LUNCH
        TemplateShortcutRole.DINNER -> MealType.DINNER
        TemplateShortcutRole.DAYTIME_SNACK -> MealType.DAYTIME_SNACK
        TemplateShortcutRole.FREE_SNACK -> MealType.FREE_SNACK
        TemplateShortcutRole.NONE -> MealType.FREE_SNACK
    }

    private fun TemplateShortcutRole.defaultTemplateName(): String = when (this) {
        TemplateShortcutRole.BREAKFAST -> "朝セット"
        TemplateShortcutRole.MORNING_SNACK -> "間朝セット"
        TemplateShortcutRole.LUNCH -> "昼セット"
        TemplateShortcutRole.DINNER -> "夜セット"
        TemplateShortcutRole.DAYTIME_SNACK -> "間昼セット"
        TemplateShortcutRole.FREE_SNACK -> "間全セット"
        TemplateShortcutRole.NONE -> "セット"
    }

    private fun driveShortcutTemplateId(role: TemplateShortcutRole): Long = when (role) {
        TemplateShortcutRole.BREAKFAST -> -1001L
        TemplateShortcutRole.MORNING_SNACK -> -1004L
        TemplateShortcutRole.LUNCH -> -1002L
        TemplateShortcutRole.DINNER -> -1003L
        TemplateShortcutRole.DAYTIME_SNACK -> -1005L
        TemplateShortcutRole.FREE_SNACK -> -1006L
        TemplateShortcutRole.NONE -> error("NONEはDriveショートカットにできません。")
    }

    companion object {
        private const val TEMPLATE_PHOTO_PREFS = "template_photo_prefs"
    }
}
