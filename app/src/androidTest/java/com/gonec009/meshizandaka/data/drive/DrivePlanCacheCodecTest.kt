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
                            items = listOf(
                                DrivePlanItem(
                                    name = "白米",
                                    amountLabel = "110 g",
                                    isMainDish = false,
                                    imageContentHash = "food-hash",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            preferredPlanId = "plan-1",
        )

        val restored = DrivePlanCacheCodec.decode(DrivePlanCacheCodec.encode(catalog))

        assertEquals(catalog, restored)
    }
}
