package com.gonec009.meshizandaka.data.drive

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveJsonObjectTest {
    @Test
    fun Windows形式のPascalCaseをAndroid側で読める() {
        val json = JSONObject(
            """
            {
              "Name": "9 月1日から（複製）",
              "IsArchived": false,
              "IsFavorite": true,
              "DisplayOrder": 3,
              "UpdatedUtcTicks": 123456789,
              "SelectedPlanId": "plan-1"
            }
            """.trimIndent(),
        )

        assertEquals("9 月1日から（複製）", json.stringOrNull("name"))
        assertFalse(json.optBooleanIgnoreCase("isArchived", true))
        assertTrue(json.optBooleanIgnoreCase("isFavorite", false))
        assertEquals(3, json.optIntIgnoreCase("displayOrder", 0))
        assertEquals(123456789L, json.optLongIgnoreCase("updatedUtcTicks", 0L))
        assertEquals("plan-1", json.stringOrNull("selectedPlanId"))
    }

    @Test
    fun 既存のcamelCaseも引き続き読める() {
        val json = JSONObject(
            """
            {
              "name": "同期プラン",
              "isArchived": true,
              "displayOrder": 2
            }
            """.trimIndent(),
        )

        assertEquals("同期プラン", json.stringOrNull("name"))
        assertTrue(json.optBooleanIgnoreCase("isArchived", false))
        assertEquals(2, json.optIntIgnoreCase("displayOrder", 0))
    }
}
