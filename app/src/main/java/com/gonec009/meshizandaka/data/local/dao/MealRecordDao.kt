package com.gonec009.meshizandaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gonec009.meshizandaka.data.local.entity.MealRecordEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordOptionEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {
    @Transaction
    @Query("SELECT * FROM meal_records ORDER BY eatenAt DESC LIMIT :limit")
    fun observeRecentRecords(limit: Int): Flow<List<MealRecordWithRelations>>

    @Transaction
    @Query("SELECT * FROM meal_records WHERE eatenAt BETWEEN :startInclusive AND :endInclusive ORDER BY eatenAt DESC")
    fun observeRecordsBetween(startInclusive: Long, endInclusive: Long): Flow<List<MealRecordWithRelations>>

    @Transaction
    @Query("SELECT * FROM meal_records WHERE id = :recordId")
    fun observeRecord(recordId: Long): Flow<MealRecordWithRelations?>

    @Query("SELECT * FROM meal_records WHERE id = :recordId")
    suspend fun getRecord(recordId: Long): MealRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MealRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordOptions(options: List<MealRecordOptionEntity>)

    @Update
    suspend fun updateRecord(record: MealRecordEntity)

    @Query("DELETE FROM meal_record_options WHERE mealRecordId = :recordId")
    suspend fun deleteOptionsForRecord(recordId: Long)

    @Query("DELETE FROM meal_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Long)
}
