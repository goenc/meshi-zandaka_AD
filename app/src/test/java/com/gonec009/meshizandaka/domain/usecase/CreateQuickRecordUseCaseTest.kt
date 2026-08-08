package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.local.dao.MealRecordDao
import com.gonec009.meshizandaka.data.local.dao.MealTemplateDao
import com.gonec009.meshizandaka.data.local.entity.MealRecordEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordOptionEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordWithRelations
import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.MealTemplateWithRelations
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupWithOptions
import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class CreateQuickRecordUseCaseTest {
    private val zoneId = ZoneId.of("Asia/Tokyo")

    @Test
    fun 朝昼夕は同日に登録すると既存記録へ追加される() = runTest {
        val templateDao = FakeMealTemplateDao(
            template = MealTemplateWithRelations(
                template = MealTemplateEntity(
                    id = 1,
                    name = "朝食テンプレート",
                    mealType = "BREAKFAST",
                    baseCalories = 400,
                    proteinG = 20.0,
                    fatG = 10.0,
                    carbG = 30.0,
                    isSpecial = false,
                    comparisonTemplateId = null,
                    weeklyLimitCount = null,
                    monthlyLimitCount = null,
                    memo = "",
                ),
                optionGroups = emptyList(),
            ),
        )
        val recordDao = FakeMealRecordDao(
            existingRecords = mutableListOf(
                MealRecordWithRelations(
                    record = MealRecordEntity(
                        id = 1,
                        eatenAt = 1780880400000,
                        mealType = "BREAKFAST",
                        templateId = 1,
                        templateNameSnapshot = "朝食テンプレート",
                        totalCalories = 400,
                        proteinG = 20.0,
                        fatG = 10.0,
                        carbG = 30.0,
                        isSpecial = false,
                        specialDeltaCalories = 0,
                        sourceType = "QUICK_BUTTON",
                        memo = "",
                    ),
                    selectedOptions = emptyList(),
                ),
            ),
        )

        val useCase = CreateQuickRecordUseCase(
            templateRepository = MealTemplateRepository(templateDao),
            recordRepository = MealRecordRepository(recordDao),
        )

        val recordId = useCase(
            templateId = 1,
            selectedOptionIds = emptyList(),
            nowMillis = 1780916400000,
            zoneId = zoneId,
        )

        val merged = recordDao.records.single().record
        assertEquals(1L, recordId)
        assertEquals(1, recordDao.records.size)
        assertEquals(800, merged.totalCalories)
        assertEquals(40.0, merged.proteinG, 0.0)
        assertEquals(20.0, merged.fatG, 0.0)
        assertEquals(60.0, merged.carbG, 0.0)
    }

    @Test
    fun 間食も同日に登録すると既存記録へ追加される() = runTest {
        val templateDao = FakeMealTemplateDao(
            template = MealTemplateWithRelations(
                template = MealTemplateEntity(
                    id = 2,
                    name = "間食テンプレート",
                    mealType = "SNACK",
                    baseCalories = 150,
                    proteinG = 5.0,
                    fatG = 5.0,
                    carbG = 20.0,
                    isSpecial = false,
                    comparisonTemplateId = null,
                    weeklyLimitCount = null,
                    monthlyLimitCount = null,
                    memo = "",
                ),
                optionGroups = emptyList(),
            ),
        )
        val recordDao = FakeMealRecordDao(
            existingRecords = mutableListOf(
                MealRecordWithRelations(
                    record = MealRecordEntity(
                        id = 1,
                        eatenAt = 1780880400000,
                        mealType = "SNACK",
                        templateId = 2,
                        templateNameSnapshot = "間食テンプレート",
                        totalCalories = 150,
                        proteinG = 5.0,
                        fatG = 5.0,
                        carbG = 20.0,
                        isSpecial = false,
                        specialDeltaCalories = 0,
                        sourceType = "QUICK_BUTTON",
                        memo = "",
                    ),
                    selectedOptions = emptyList(),
                ),
            ),
        )

        val useCase = CreateQuickRecordUseCase(
            templateRepository = MealTemplateRepository(templateDao),
            recordRepository = MealRecordRepository(recordDao),
        )

        val insertedId = useCase(
            templateId = 2,
            selectedOptionIds = emptyList(),
            nowMillis = 1780916400000,
            zoneId = zoneId,
        )

        val merged = recordDao.records.single().record
        assertEquals(1L, insertedId)
        assertEquals(1, recordDao.records.size)
        assertEquals(300, merged.totalCalories)
        assertEquals(10.0, merged.proteinG, 0.0)
        assertEquals(10.0, merged.fatG, 0.0)
        assertEquals(40.0, merged.carbG, 0.0)
    }

    @Test
    fun 外食カードを既存の昼食へ追加できる() = runTest {
        val templateDao = FakeMealTemplateDao(
            template = MealTemplateWithRelations(
                template = MealTemplateEntity(
                    id = 3,
                    name = "外食カード",
                    mealType = "EATING_OUT",
                    baseCalories = 600,
                    proteinG = 25.0,
                    fatG = 20.0,
                    carbG = 70.0,
                    isSpecial = true,
                    comparisonTemplateId = null,
                    weeklyLimitCount = null,
                    monthlyLimitCount = null,
                    memo = "",
                ),
                optionGroups = emptyList(),
            ),
        )
        val recordDao = FakeMealRecordDao(
            existingRecords = mutableListOf(
                MealRecordWithRelations(
                    record = MealRecordEntity(
                        id = 1,
                        eatenAt = 1780880400000,
                        mealType = "LUNCH",
                        templateId = 10,
                        templateNameSnapshot = "昼食",
                        totalCalories = 500,
                        proteinG = 20.0,
                        fatG = 10.0,
                        carbG = 50.0,
                        isSpecial = false,
                        specialDeltaCalories = 0,
                        sourceType = "TEMPLATE",
                        memo = "昼のメモ",
                    ),
                    selectedOptions = emptyList(),
                ),
            ),
        )
        val useCase = CreateQuickRecordUseCase(
            templateRepository = MealTemplateRepository(templateDao),
            recordRepository = MealRecordRepository(recordDao),
        )

        val recordId = useCase(
            templateId = 3,
            nowMillis = 1780880400000,
            zoneId = zoneId,
        )

        val merged = recordDao.records.single().record
        assertEquals(1L, recordId)
        assertEquals(1, recordDao.records.size)
        assertEquals("昼食 / 外食カード", merged.templateNameSnapshot)
        assertEquals(1100, merged.totalCalories)
        assertEquals(45.0, merged.proteinG, 0.0)
        assertEquals(30.0, merged.fatG, 0.0)
        assertEquals(120.0, merged.carbG, 0.0)
        assertEquals("昼のメモ", merged.memo)
        assertEquals("LUNCH", merged.mealType)
        assertEquals(600, merged.specialDeltaCalories)
    }

    @Test
    fun Drive外食カードはテンプレートなしで新規記録できる() = runTest {
        val templateDao = FakeMealTemplateDao(
            template = MealTemplateWithRelations(
                template = MealTemplateEntity(
                    id = 3,
                    name = "未使用テンプレート",
                    mealType = "EATING_OUT",
                    baseCalories = 0,
                    proteinG = 0.0,
                    fatG = 0.0,
                    carbG = 0.0,
                    isSpecial = false,
                    comparisonTemplateId = null,
                    weeklyLimitCount = null,
                    monthlyLimitCount = null,
                    memo = "",
                ),
                optionGroups = emptyList(),
            ),
        )
        val recordDao = FakeMealRecordDao(existingRecords = mutableListOf())
        val useCase = CreateQuickRecordUseCase(
            templateRepository = MealTemplateRepository(templateDao),
            recordRepository = MealRecordRepository(recordDao),
        )

        val recordId = useCase(
            templateId = null,
            templateNameSnapshot = "新規外食",
            totalCaloriesOverride = 511,
            proteinOverride = 17.0,
            fatOverride = 30.2,
            carbOverride = 43.0,
            isSpecialOverride = true,
            memo = "1 個",
            mealType = MealType.EATING_OUT,
            nowMillis = 1780880400000,
            zoneId = zoneId,
        )

        val record = recordDao.records.single().record
        assertEquals(1L, recordId)
        assertNull(record.templateId)
        assertEquals("新規外食", record.templateNameSnapshot)
        assertEquals(511, record.totalCalories)
        assertEquals("EATING_OUT", record.mealType)
        assertEquals(511, record.specialDeltaCalories)
    }
}

private class FakeMealTemplateDao(
    private val template: MealTemplateWithRelations,
) : MealTemplateDao {
    override fun observeActiveTemplates(): Flow<List<MealTemplateWithRelations>> = emptyFlow()

    override fun observeNormalTemplates(): Flow<List<MealTemplateEntity>> = emptyFlow()

    override suspend fun getTemplate(templateId: Long): MealTemplateWithRelations? = template.takeIf { it.template.id == templateId }

    override suspend fun countTemplates(): Int = 1

    override suspend fun insertTemplate(template: MealTemplateEntity): Long = template.id

    override suspend fun insertTemplates(templates: List<MealTemplateEntity>): List<Long> = templates.map { it.id }

    override suspend fun insertOptionGroups(groups: List<TemplateOptionGroupEntity>): List<Long> = groups.map { it.id }

    override suspend fun insertOptions(options: List<TemplateOptionEntity>) = Unit

    override suspend fun updateTemplate(template: MealTemplateEntity) = Unit

    override suspend fun deactivateTemplate(templateId: Long) = Unit

    override suspend fun deactivateActiveTemplatesByShortcutRole(shortcutRole: String) = Unit
}

private class FakeMealRecordDao(
    existingRecords: MutableList<MealRecordWithRelations>,
) : MealRecordDao {
    val records = existingRecords

    override fun observeRecentRecords(limit: Int): Flow<List<MealRecordWithRelations>> = emptyFlow()

    override fun observeRecordsBetween(startInclusive: Long, endInclusive: Long): Flow<List<MealRecordWithRelations>> = emptyFlow()

    override suspend fun getRecordsBetween(
        startInclusive: Long,
        endInclusive: Long,
    ): List<MealRecordWithRelations> = records
        .filter { it.record.eatenAt in startInclusive..endInclusive }
        .sortedByDescending { it.record.eatenAt }

    override fun observeRecord(recordId: Long): Flow<MealRecordWithRelations?> = emptyFlow()

    override suspend fun getRecord(recordId: Long): MealRecordEntity? = records.firstOrNull { it.record.id == recordId }?.record

    override suspend fun getRecordWithRelations(recordId: Long): MealRecordWithRelations? =
        records.firstOrNull { it.record.id == recordId }

    override suspend fun existsRecordForMealTypeBetween(
        mealType: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Boolean {
        return records.any { item ->
            item.record.mealType == mealType && item.record.eatenAt in startInclusive..endInclusive
        }
    }

    override suspend fun insertRecord(record: MealRecordEntity): Long {
        val nextId = (records.maxOfOrNull { it.record.id } ?: 0L) + 1L
        records += MealRecordWithRelations(record = record.copy(id = nextId), selectedOptions = emptyList())
        return nextId
    }

    override suspend fun insertRecordOptions(options: List<MealRecordOptionEntity>) = Unit

    override suspend fun updateRecord(record: MealRecordEntity) {
        val index = records.indexOfFirst { it.record.id == record.id }
        if (index >= 0) {
            records[index] = records[index].copy(record = record)
        }
    }

    override suspend fun deleteOptionsForRecord(recordId: Long) = Unit

    override suspend fun deleteRecord(recordId: Long) = Unit
}
