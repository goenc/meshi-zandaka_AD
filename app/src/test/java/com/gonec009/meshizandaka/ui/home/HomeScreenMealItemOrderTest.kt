package com.gonec009.meshizandaka.ui.home

import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.recordKey
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenMealItemOrderTest {
    @Test
    fun 選択中の主菜を先頭へ移動し未選択候補を末尾へ移動する() {
        val chicken = DrivePlanItem(
            name = "鶏もも肉",
            amountLabel = "80 g",
            isMainDish = true,
            isMainDishCandidate = true,
            id = "chicken",
        )
        val rice = DrivePlanItem(
            name = "白米",
            amountLabel = "110 g",
            isMainDish = false,
            id = "rice",
        )
        val salmon = DrivePlanItem(
            name = "鮭",
            amountLabel = "100 g",
            isMainDish = false,
            isMainDishCandidate = true,
            id = "salmon",
        )

        val ordered = orderDrivePlanMealItems(
            itemEntries = listOf(
                chicken to chicken.recordKey(0),
                rice to rice.recordKey(1),
                salmon to salmon.recordKey(2),
            ),
            selectedMainDishKey = salmon.recordKey(2),
        )

        assertEquals(listOf("鮭", "白米", "鶏もも肉"), ordered.map { (item, _) -> item.name })
    }
}
