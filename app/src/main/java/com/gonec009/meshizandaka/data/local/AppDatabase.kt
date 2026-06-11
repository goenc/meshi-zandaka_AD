package com.gonec009.meshizandaka.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gonec009.meshizandaka.data.local.dao.MealRecordDao
import com.gonec009.meshizandaka.data.local.dao.MealTemplateDao
import com.gonec009.meshizandaka.data.local.entity.MealRecordEntity
import com.gonec009.meshizandaka.data.local.entity.MealRecordOptionEntity
import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity

@Database(
    entities = [
        MealTemplateEntity::class,
        TemplateOptionGroupEntity::class,
        TemplateOptionEntity::class,
        MealRecordEntity::class,
        MealRecordOptionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun mealRecordDao(): MealRecordDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "meshi_zandaka.db",
            ).fallbackToDestructiveMigration().build()
        }
    }
}
