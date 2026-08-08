package com.gonec009.meshizandaka.data.drive

import org.junit.Assert.assertEquals
import org.junit.Test

class DrivePlanCacheCodecTest {
    @Test
    fun プラン一覧を保存用JSONへ変換して復元できる() {
        val catalog = DrivePlanCatalog(
            plans = listOf(
                DrivePlan(
                    id = "plan-1",
                    name = "9 月1日から（複製）",
                    targetDate = "2026-09-01",
                    memo = "Windowsから同期",
                    isFavorite = true,
                    displayOrder = 2,
                    updatedUtcTicks = 123456789L,
                    meals = listOf(
                        DrivePlanMeal(
                            slot = 0,
                            label = "朝のテンプレート",
                            name = "朝食",
                            memo = "メモ",
                            imageContentHash = "meal-hash",
                            totalCalories = 425,
                            proteinG = 33.0,
                            fatG = 6.4,
                            carbG = 65.8,
                            nutritionDataAvailable = true,
                            items = listOf(
                                DrivePlanItem(
                                    name = "白米",
                                    amountLabel = "110 g",
                                    isMainDish = false,
                                    imageContentHash = "food-hash",
                                    calories = 172,
                                    proteinG = 2.8,
                                    fatG = 0.3,
                                    carbG = 38.1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            preferredPlanId = "plan-1",
            externalCards = listOf(
                DriveExternalCard(
                    id = "card-1",
                    name = "ビックマックサラダ",
                    storeName = "マック",
                    amountLabel = "1 個",
                    memo = "外食カード",
                    imageContentHash = "card-image-hash",
                    imagePath = "/data/user/0/com.gonec009.meshizandaka/files/card.jpg",
                    calories = 534,
                    proteinG = 26.6,
                    fatG = 28.1,
                    carbG = 44.3,
                ),
            ),
        )

        val restored = DrivePlanCacheCodec.decode(DrivePlanCacheCodec.encode(catalog))

        assertEquals(catalog, restored)
    }
}
