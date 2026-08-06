package com.gonec009.meshizandaka.domain.model

enum class MealType {
    BREAKFAST,
    MORNING_SNACK,
    LUNCH,
    DINNER,
    DAYTIME_SNACK,
    FREE_SNACK,
    // 旧DB互換。新規入力ではFREE_SNACKを使用する。
    SNACK,
    EATING_OUT,
}
