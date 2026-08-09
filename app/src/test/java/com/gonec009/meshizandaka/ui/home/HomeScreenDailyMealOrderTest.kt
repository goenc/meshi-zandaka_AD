package com.gonec009.meshizandaka.ui.home

import com.gonec009.meshizandaka.domain.model.DailyMealStack
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeScreenDailyMealOrderTest {
    @Test
    fun 日別食事一覧を食事区分の指定順に並べる() {
        val breakfast = record(MealType.BREAKFAST, "朝食")
        val lunch = record(MealType.LUNCH, "昼食")
        val dinner = record(MealType.DINNER, "夕食")
        val morningSnack = record(MealType.MORNING_SNACK, "朝の間食")
        val daytimeSnack = record(MealType.DAYTIME_SNACK, "昼の間食")
        val freeSnack = record(MealType.FREE_SNACK, "フリー間食")

        val ordered = DailyMealStack(
            date = LocalDate.of(2026, 8, 9),
            breakfastRecords = listOf(breakfast),
            lunchRecords = listOf(lunch),
            dinnerRecords = listOf(dinner),
            morningSnackRecords = listOf(morningSnack),
            daytimeSnackRecords = listOf(daytimeSnack),
            freeSnackRecords = listOf(freeSnack),
        ).allRecords()

        assertEquals(
            listOf(
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.DINNER,
                MealType.MORNING_SNACK,
                MealType.DAYTIME_SNACK,
                MealType.FREE_SNACK,
            ),
            ordered.map(MealRecord::mealType),
        )
    }

    private fun record(mealType: MealType, name: String): MealRecord {
        return MealRecord(
            eatenAt = 0,
            mealType = mealType,
            templateId = null,
            templateNameSnapshot = name,
            totalCalories = 0,
            proteinG = 0.0,
            fatG = 0.0,
            carbG = 0.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
        )
    }
}
