package com.gonec009.meshizandaka.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.WeekStartDay

@Composable
fun mealTypeLabel(mealType: MealType): String = stringResource(
    when (mealType) {
        MealType.BREAKFAST -> R.string.meal_type_breakfast
        MealType.LUNCH -> R.string.meal_type_lunch
        MealType.DINNER -> R.string.meal_type_dinner
        MealType.SNACK -> R.string.meal_type_snack
        MealType.EATING_OUT -> R.string.meal_type_eating_out
    },
)

@Composable
fun weekStartDayLabel(weekStartDay: WeekStartDay): String = stringResource(
    when (weekStartDay) {
        WeekStartDay.MONDAY -> R.string.week_start_monday
        WeekStartDay.SUNDAY -> R.string.week_start_sunday
    },
)
