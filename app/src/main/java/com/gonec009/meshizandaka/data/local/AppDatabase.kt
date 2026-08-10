package com.gonec009.meshizandaka.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gonec009.meshizandaka.data.local.dao.MealRecordDao
import com.gonec009.meshizandaka.data.local.dao.MealTemplateDao
import com.gonec009.meshizandaka.data.local.dao.DrivePlanCacheDao
import com.gonec009.meshizandaka.data.local.entity.MealRecordEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordOptionEntity
import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import com.gonec009.meshizandaka.data.local.entity.DrivePlanCacheEntity

@Database(
    entities = [
        MealTemplateEntity::class,
        TemplateOptionGroupEntity::class,
        TemplateOptionEntity::class,
        MealRecordEntity::class,
        MealRecordOptionEntity::class,
        DrivePlanCacheEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun drivePlanCacheDao(): DrivePlanCacheDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "meshi_zandaka.db",
            )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS drive_plan_cache (
                        datasetId TEXT NOT NULL,
                        catalogJson TEXT NOT NULL,
                        selectedPlanId TEXT,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(datasetId)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_records ADD COLUMN excludedDrivePlanItemKeysJson TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_records ADD COLUMN selectedDrivePlanMainDishItemKey TEXT DEFAULT NULL",
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE meal_records ADD COLUMN editedOptionGroupNamesJson TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }
    }
}
