package com.gonec009.meshizandaka.data.drive

enum class DrivePlanPhase {
    IDLE,
    LOADING,
    READY,
    FAILED,
}

data class DrivePlanItem(
    val name: String,
    val amountLabel: String,
    val isMainDish: Boolean,
    val isMainDishCandidate: Boolean = false,
    val imageContentHash: String? = null,
    val imagePath: String? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val id: String? = null,
)

data class DrivePlanMeal(
    val slot: Int,
    val label: String,
    val name: String,
    val memo: String,
    val imageContentHash: String? = null,
    val imagePath: String? = null,
    val items: List<DrivePlanItem> = emptyList(),
    val totalCalories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val nutritionDataAvailable: Boolean = false,
)

data class DrivePlan(
    val id: String,
    val name: String,
    val targetDate: String?,
    val memo: String,
    val isFavorite: Boolean,
    val displayOrder: Int,
    val updatedUtcTicks: Long,
    val meals: List<DrivePlanMeal>,
)

data class DriveExternalCard(
    val id: String,
    val name: String,
    val storeName: String? = null,
    val tabName: String? = null,
    val amountLabel: String = "1 個",
    val memo: String = "",
    val imageContentHash: String? = null,
    val imagePath: String? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val items: List<DrivePlanItem> = emptyList(),
)

data class DriveFood(
    val id: String,
    val name: String,
    val mealCategory: Int,
    val amountLabel: String = "",
    val imageContentHash: String? = null,
    val imagePath: String? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
)

data class DrivePlanCatalog(
    val plans: List<DrivePlan>,
    val preferredPlanId: String?,
    val externalCards: List<DriveExternalCard> = emptyList(),
    val foods: List<DriveFood> = emptyList(),
)

data class DrivePlanState(
    val phase: DrivePlanPhase = DrivePlanPhase.IDLE,
    val plans: List<DrivePlan> = emptyList(),
    val externalCards: List<DriveExternalCard> = emptyList(),
    val foods: List<DriveFood> = emptyList(),
    val selectedPlanId: String? = null,
    val imageLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedPlan: DrivePlan?
        get() = plans.firstOrNull { plan -> plan.id == selectedPlanId }
}

fun DrivePlanItem.recordKey(index: Int): String {
    return id?.takeIf { it.isNotBlank() }
        ?: "${name}\u001F${amountLabel}\u001F$index"
}

internal fun DrivePlanItem.isIncludedInMealNutrition(): Boolean {
    return !isMainDishCandidate || isMainDish
}

fun drivePlanMealLabel(slot: Int): String = when (slot) {
    0 -> "朝のテンプレート"
    1 -> "間朝のテンプレート"
    2 -> "昼のテンプレート"
    3 -> "夜のテンプレート"
    4 -> "間昼のテンプレート"
    5 -> "間全のテンプレート"
    else -> "食事テンプレート"
}
