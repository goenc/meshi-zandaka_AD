package com.gonec009.meshizandaka.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.local.dao.MealRecordDao
import com.gonec009.meshizandaka.data.local.entity.MealRecordEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordOptionEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordWithRelations
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MealRecordRepository(
    private val dao: MealRecordDao,
    private val context: Context? = null,
) {
    fun observeRecentRecords(limit: Int = 10): Flow<List<MealRecord>> =
        dao.observeRecentRecords(limit).map { items -> items.map(::toModel) }

    fun observeRecordsBetween(startInclusive: Long, endInclusive: Long): Flow<List<MealRecord>> =
        dao.observeRecordsBetween(startInclusive, endInclusive).map { items -> items.map(::toModel) }

    suspend fun getRecordsBetween(startInclusive: Long, endInclusive: Long): List<MealRecord> =
        dao.getRecordsBetween(startInclusive, endInclusive).map(::toModel)

    fun observeRecord(recordId: Long): Flow<MealRecord?> = dao.observeRecord(recordId).map { it?.let(::toModel) }

    suspend fun getRecord(recordId: Long): MealRecord? = dao.getRecordWithRelations(recordId)?.let(::toModel)

    suspend fun existsRecordForMealTypeBetween(
        mealType: MealType,
        startInclusive: Long,
        endInclusive: Long,
    ): Boolean = dao.existsRecordForMealTypeBetween(mealType.name, startInclusive, endInclusive)

    suspend fun insertRecord(record: MealRecord): Long {
        val recordId = dao.insertRecord(
            MealRecordEntity(
                eatenAt = record.eatenAt,
                mealType = record.mealType.name,
                templateId = record.templateId,
                templateNameSnapshot = record.templateNameSnapshot,
                totalCalories = record.totalCalories,
                proteinG = record.proteinG,
                fatG = record.fatG,
                carbG = record.carbG,
                isSpecial = record.isSpecial,
                specialDeltaCalories = record.specialDeltaCalories,
                sourceType = record.sourceType.name,
                memo = record.memo,
                photoUri = record.photoUri,
                excludedDrivePlanItemKeysJson = encodeExcludedDrivePlanItemKeys(record.excludedDrivePlanItemKeys),
                selectedDrivePlanMainDishItemKey = record.selectedDrivePlanMainDishItemKey,
            ),
        )
        if (record.selectedOptions.isNotEmpty()) {
            dao.insertRecordOptions(record.selectedOptions.toEntities(recordId))
        }
        return recordId
    }

    suspend fun updateRecord(record: MealRecord) {
        val current = dao.getRecord(record.id) ?: return
        dao.updateRecord(
            current.copy(
                eatenAt = record.eatenAt,
                mealType = record.mealType.name,
                templateId = record.templateId,
                templateNameSnapshot = record.templateNameSnapshot,
                totalCalories = record.totalCalories,
                proteinG = record.proteinG,
                fatG = record.fatG,
                carbG = record.carbG,
                isSpecial = record.isSpecial,
                specialDeltaCalories = record.specialDeltaCalories,
                memo = record.memo,
                sourceType = record.sourceType.name,
                photoUri = record.photoUri,
                excludedDrivePlanItemKeysJson = encodeExcludedDrivePlanItemKeys(record.excludedDrivePlanItemKeys),
                selectedDrivePlanMainDishItemKey = record.selectedDrivePlanMainDishItemKey,
            ),
        )
        dao.deleteOptionsForRecord(record.id)
        if (record.selectedOptions.isNotEmpty()) {
            dao.insertRecordOptions(record.selectedOptions.toEntities(record.id))
        }
        if (current.photoUri != record.photoUri) {
            deletePhoto(current.photoUri)
        }
    }

    suspend fun appendToRecord(recordId: Long, addition: MealRecord): Boolean {
        val current = getRecord(recordId) ?: return false
        val combinedName = listOf(current.templateNameSnapshot, addition.templateNameSnapshot)
            .filter { it.isNotBlank() }
            .joinToString(" / ")
        val combinedMemo = listOf(current.memo, addition.memo)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        updateRecord(
            current.copy(
                templateNameSnapshot = combinedName,
                totalCalories = current.totalCalories + addition.totalCalories,
                proteinG = current.proteinG + addition.proteinG,
                fatG = current.fatG + addition.fatG,
                carbG = current.carbG + addition.carbG,
                isSpecial = current.isSpecial || addition.isSpecial,
                specialDeltaCalories = current.specialDeltaCalories + addition.specialDeltaCalories,
                memo = combinedMemo,
                photoUri = current.photoUri ?: addition.photoUri,
                selectedOptions = current.selectedOptions + addition.selectedOptions,
                selectedDrivePlanMainDishItemKey = current.selectedDrivePlanMainDishItemKey
                    ?: addition.selectedDrivePlanMainDishItemKey,
            ),
        )
        return true
    }

    suspend fun excludeDrivePlanItem(
        recordId: Long,
        itemKey: String,
        calories: Int,
        proteinG: Double,
        fatG: Double,
        carbG: Double,
    ): Boolean {
        if (itemKey.isBlank()) return false
        val current = getRecord(recordId) ?: return false
        if (itemKey in current.excludedDrivePlanItemKeys) return false

        updateRecord(
            current.copy(
                totalCalories = (current.totalCalories - calories).coerceAtLeast(0),
                proteinG = (current.proteinG - proteinG).coerceAtLeast(0.0),
                fatG = (current.fatG - fatG).coerceAtLeast(0.0),
                carbG = (current.carbG - carbG).coerceAtLeast(0.0),
                specialDeltaCalories = if (current.isSpecial) {
                    current.specialDeltaCalories - calories
                } else {
                    current.specialDeltaCalories
                },
                excludedDrivePlanItemKeys = current.excludedDrivePlanItemKeys + itemKey,
                selectedDrivePlanMainDishItemKey = current.selectedDrivePlanMainDishItemKey
                    .takeUnless { it == itemKey },
            ),
        )
        return true
    }

    suspend fun selectDrivePlanMainDish(
        recordId: Long,
        currentItem: DrivePlanItem?,
        selectedItem: DrivePlanItem,
        selectedItemKey: String,
    ): Boolean {
        if (selectedItemKey.isBlank()) return false
        val current = getRecord(recordId) ?: return false
        if (current.selectedDrivePlanMainDishItemKey == selectedItemKey) return false
        val calorieDelta = selectedItem.calories - (currentItem?.calories ?: 0)
        val proteinDelta = selectedItem.proteinG - (currentItem?.proteinG ?: 0.0)
        val fatDelta = selectedItem.fatG - (currentItem?.fatG ?: 0.0)
        val carbDelta = selectedItem.carbG - (currentItem?.carbG ?: 0.0)
        updateRecord(
            current.copy(
                totalCalories = (current.totalCalories + calorieDelta).coerceAtLeast(0),
                proteinG = (current.proteinG + proteinDelta).coerceAtLeast(0.0),
                fatG = (current.fatG + fatDelta).coerceAtLeast(0.0),
                carbG = (current.carbG + carbDelta).coerceAtLeast(0.0),
                specialDeltaCalories = if (current.isSpecial) {
                    current.specialDeltaCalories + calorieDelta
                } else {
                    current.specialDeltaCalories
                },
                selectedDrivePlanMainDishItemKey = selectedItemKey,
            ),
        )
        return true
    }

    suspend fun deleteRecordOption(recordId: Long, option: MealRecordOption): Boolean {
        if (option.id <= 0L) return false
        val current = getRecord(recordId) ?: return false
        val storedOption = current.selectedOptions.firstOrNull { it.id == option.id } ?: return false
        updateRecord(
            current.copy(
                totalCalories = (current.totalCalories - storedOption.calorieDelta).coerceAtLeast(0),
                proteinG = (current.proteinG - storedOption.proteinDeltaG).coerceAtLeast(0.0),
                fatG = (current.fatG - storedOption.fatDeltaG).coerceAtLeast(0.0),
                carbG = (current.carbG - storedOption.carbDeltaG).coerceAtLeast(0.0),
                specialDeltaCalories = if (current.isSpecial) {
                    current.specialDeltaCalories - storedOption.calorieDelta
                } else {
                    current.specialDeltaCalories
                },
                selectedOptions = current.selectedOptions.filterNot { it.id == storedOption.id },
            ),
        )
        return true
    }

    suspend fun deleteRecord(recordId: Long) {
        val photoUri = dao.getRecord(recordId)?.photoUri
        dao.deleteRecord(recordId)
        deletePhoto(photoUri)
    }

    private suspend fun deletePhoto(photoUri: String?) {
        val appContext = context ?: return
        if (photoUri.isNullOrBlank()) return
        if (runCatching { photoUri.toUri() }.getOrNull()?.path?.contains("/template_photos/") == true) return
        withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.delete(photoUri.toUri(), null, null)
            }
        }
    }

    private fun toModel(item: MealRecordWithRelations): MealRecord {
        return MealRecord(
            id = item.record.id,
            eatenAt = item.record.eatenAt,
            mealType = MealType.valueOf(item.record.mealType),
            templateId = item.record.templateId,
            templateNameSnapshot = item.record.templateNameSnapshot,
            totalCalories = item.record.totalCalories,
            proteinG = item.record.proteinG,
            fatG = item.record.fatG,
            carbG = item.record.carbG,
            isSpecial = item.record.isSpecial,
            specialDeltaCalories = item.record.specialDeltaCalories,
            sourceType = SourceType.valueOf(item.record.sourceType),
            memo = item.record.memo,
            photoUri = item.record.photoUri,
            excludedDrivePlanItemKeys = decodeExcludedDrivePlanItemKeys(
                item.record.excludedDrivePlanItemKeysJson,
            ),
            selectedDrivePlanMainDishItemKey = item.record.selectedDrivePlanMainDishItemKey,
            selectedOptions = item.selectedOptions.map { option ->
                MealRecordOption(
                    id = option.id,
                    mealRecordId = option.mealRecordId,
                    optionGroupNameSnapshot = option.optionGroupNameSnapshot,
                    optionNameSnapshot = option.optionNameSnapshot,
                    calorieDelta = option.calorieDelta,
                    proteinDeltaG = option.proteinDeltaG,
                    fatDeltaG = option.fatDeltaG,
                    carbDeltaG = option.carbDeltaG,
                )
            },
        )
    }

    private fun List<MealRecordOption>.toEntities(recordId: Long): List<MealRecordOptionEntity> =
        map { option ->
            MealRecordOptionEntity(
                mealRecordId = recordId,
                optionGroupNameSnapshot = option.optionGroupNameSnapshot,
                optionNameSnapshot = option.optionNameSnapshot,
                calorieDelta = option.calorieDelta,
                proteinDeltaG = option.proteinDeltaG,
                fatDeltaG = option.fatDeltaG,
                carbDeltaG = option.carbDeltaG,
            )
        }

    private fun encodeExcludedDrivePlanItemKeys(keys: Set<String>): String {
        val nonBlankKeys = keys.filter { it.isNotBlank() }
        if (nonBlankKeys.isEmpty()) return "[]"
        return JSONArray().apply {
            nonBlankKeys.forEach { put(it) }
        }.toString()
    }

    private fun decodeExcludedDrivePlanItemKeys(value: String): Set<String> {
        return runCatching {
            val array = JSONArray(value)
            buildSet {
                repeat(array.length()) {
                    array.optString(it).takeIf { key -> key.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }
}
