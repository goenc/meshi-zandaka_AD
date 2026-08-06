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
    val imageContentHash: String? = null,
    val imagePath: String? = null,
)

data class DrivePlanMeal(
    val slot: Int,
    val label: String,
    val name: String,
    val memo: String,
    val imageContentHash: String? = null,
    val imagePath: String? = null,
    val items: List<DrivePlanItem> = emptyList(),
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

data class DrivePlanCatalog(
    val plans: List<DrivePlan>,
    val preferredPlanId: String?,
)

data class DrivePlanState(
    val phase: DrivePlanPhase = DrivePlanPhase.IDLE,
    val plans: List<DrivePlan> = emptyList(),
    val selectedPlanId: String? = null,
    val imageLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedPlan: DrivePlan?
        get() = plans.firstOrNull { plan -> plan.id == selectedPlanId }
}

fun drivePlanMealLabel(slot: Int): String = when (slot) {
    0 -> "朝のテンプレート"
    1 -> "間食テンプレート1"
    2 -> "昼のテンプレート"
    3 -> "夜のテンプレート"
    4 -> "間食テンプレート2"
    5 -> "間食テンプレート3"
    else -> "食事テンプレート"
}
