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

internal fun DrivePlanCatalog.preserveImagePathsFrom(previous: DrivePlanState): DrivePlanCatalog {
    val previousPlans = previous.plans.associateBy { plan -> plan.id }
    val previousCards = previous.externalCards.associateBy { card -> card.id }
    val previousFoods = previous.foods.associateBy { food -> food.id }
    return copy(
        plans = plans.map { plan -> plan.preserveImagePathsFrom(previousPlans[plan.id]) },
        externalCards = externalCards.map { card ->
            card.preserveImagePathsFrom(previousCards[card.id])
        },
        foods = foods.map { food ->
            val previousFood = previousFoods[food.id]
            food.copy(
                imagePath = preservedImagePath(
                    currentHash = food.imageContentHash,
                    currentPath = food.imagePath,
                    previousHash = previousFood?.imageContentHash,
                    previousPath = previousFood?.imagePath,
                ),
            )
        },
    )
}

private fun DrivePlan.preserveImagePathsFrom(previous: DrivePlan?): DrivePlan {
    val previousMeals = previous?.meals?.associateBy { meal -> meal.slot }.orEmpty()
    return copy(
        meals = meals.map { meal ->
            val previousMeal = previousMeals[meal.slot]
            meal.copy(
                imagePath = preservedImagePath(
                    currentHash = meal.imageContentHash,
                    currentPath = meal.imagePath,
                    previousHash = previousMeal?.imageContentHash,
                    previousPath = previousMeal?.imagePath,
                ),
                items = meal.items.map { item ->
                    val previousItem = previousMeal?.items?.firstOrNull { candidate ->
                        sameImageHash(item.imageContentHash, candidate.imageContentHash) &&
                            !candidate.imagePath.isNullOrBlank()
                    }
                    item.copy(
                        imagePath = preservedImagePath(
                            currentHash = item.imageContentHash,
                            currentPath = item.imagePath,
                            previousHash = previousItem?.imageContentHash,
                            previousPath = previousItem?.imagePath,
                        ),
                    )
                },
            )
        },
    )
}

private fun DriveExternalCard.preserveImagePathsFrom(previous: DriveExternalCard?): DriveExternalCard {
    return copy(
        imagePath = preservedImagePath(
            currentHash = imageContentHash,
            currentPath = imagePath,
            previousHash = previous?.imageContentHash,
            previousPath = previous?.imagePath,
        ),
        items = items.map { item ->
            val previousItem = previous?.items?.firstOrNull { candidate ->
                sameImageHash(item.imageContentHash, candidate.imageContentHash) &&
                    !candidate.imagePath.isNullOrBlank()
            }
            item.copy(
                imagePath = preservedImagePath(
                    currentHash = item.imageContentHash,
                    currentPath = item.imagePath,
                    previousHash = previousItem?.imageContentHash,
                    previousPath = previousItem?.imagePath,
                ),
            )
        },
    )
}

private fun preservedImagePath(
    currentHash: String?,
    currentPath: String?,
    previousHash: String?,
    previousPath: String?,
): String? {
    if (!currentPath.isNullOrBlank()) return currentPath
    return previousPath.takeIf { path -> !path.isNullOrBlank() && sameImageHash(currentHash, previousHash) }
}

private fun sameImageHash(first: String?, second: String?): Boolean {
    return !first.isNullOrBlank() && !second.isNullOrBlank() && first.equals(second, ignoreCase = true)
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
