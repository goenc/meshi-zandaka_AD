package com.gonec009.meshizandaka.data.drive

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Windows側の同期ログはエンベロープと行PayloadでJSONの大文字小文字が異なるため、
 * Android側ではキー名を大小文字非依存で読む。camelCaseもそのまま利用できる。
 */
internal fun JSONObject.valueIgnoreCase(key: String): Any? {
    val iterator = keys()
    while (iterator.hasNext()) {
        val actualKey = iterator.next()
        if (actualKey.equals(key, ignoreCase = true)) {
            return opt(actualKey).takeUnless { it == JSONObject.NULL }
        }
    }
    return null
}

internal fun JSONObject.stringOrNull(key: String): String? =
    valueIgnoreCase(key)
        ?.toString()
        ?.takeIf { it.isNotBlank() }

internal fun JSONObject.optJSONArrayIgnoreCase(key: String): JSONArray? =
    valueIgnoreCase(key) as? JSONArray

internal fun JSONObject.optJSONObjectIgnoreCase(key: String): JSONObject? =
    valueIgnoreCase(key) as? JSONObject

internal fun JSONObject.optBooleanIgnoreCase(key: String, default: Boolean): Boolean =
    when (val value = valueIgnoreCase(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> when (value.lowercase(Locale.ROOT)) {
            "true", "1" -> true
            "false", "0" -> false
            else -> default
        }
        else -> default
    }

internal fun JSONObject.optIntIgnoreCase(key: String, default: Int): Int =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> default
    }

internal fun JSONObject.optLongIgnoreCase(key: String, default: Long): Long =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: default
        else -> default
    }

internal fun JSONObject.longOrNullIgnoreCase(key: String): Long? =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
