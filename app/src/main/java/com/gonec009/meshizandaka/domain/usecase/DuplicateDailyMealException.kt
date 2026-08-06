package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.domain.model.MealType

class DuplicateDailyMealException(
    mealType: MealType,
) : IllegalStateException("${mealTypeLabel(mealType)}は今日はすでに登録されています。削除してから登録してください。")

private fun mealTypeLabel(mealType: MealType): String {
    return when (mealType) {
        MealType.BREAKFAST -> "朝食"
        MealType.MORNING_SNACK -> "間朝"
        MealType.LUNCH -> "昼食"
        MealType.DINNER -> "夕食"
        MealType.DAYTIME_SNACK -> "間昼"
        MealType.FREE_SNACK,
        MealType.SNACK,
        -> "間全"
        MealType.EATING_OUT -> "外食"
    }
}
