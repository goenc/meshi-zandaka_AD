package com.gonec009.meshizandaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.MealTemplateWithRelations
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {
    @Transaction
    @Query("SELECT * FROM meal_templates WHERE isActive = 1 ORDER BY isSpecial ASC, name ASC")
    fun observeActiveTemplates(): Flow<List<MealTemplateWithRelations>>

    @Query("SELECT * FROM meal_templates WHERE isActive = 1 AND isSpecial = 0 ORDER BY name ASC")
    fun observeNormalTemplates(): Flow<List<MealTemplateEntity>>

    @Transaction
    @Query("SELECT * FROM meal_templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: Long): MealTemplateWithRelations?

    @Query("SELECT COUNT(*) FROM meal_templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MealTemplateEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptionGroups(groups: List<TemplateOptionGroupEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<TemplateOptionEntity>)

    @Update
    suspend fun updateTemplate(template: MealTemplateEntity)

    @Query("UPDATE meal_templates SET isActive = 0 WHERE id = :templateId")
    suspend fun deactivateTemplate(templateId: Long)

    @Query("UPDATE meal_templates SET isActive = 0 WHERE shortcutRole = :shortcutRole AND isActive = 1")
    suspend fun deactivateActiveTemplatesByShortcutRole(shortcutRole: String)
}
