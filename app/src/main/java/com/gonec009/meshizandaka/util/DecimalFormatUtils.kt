package com.gonec009.meshizandaka.util

import java.util.Locale

fun sanitizeDecimalInput(value: String): String {
    if (value.isEmpty()) return value
    val normalized = buildString {
        var seenDot = false
        value.forEach { character ->
            when {
                character.isDigit() -> append(character)
                character == '.' && !seenDot -> {
                    append(character)
                    seenDot = true
                }
            }
        }
    }
    val dotIndex = normalized.indexOf('.')
    return if (dotIndex >= 0) {
        normalized.substring(0, dotIndex + 1) + normalized.substring(dotIndex + 1).take(1)
    } else {
        normalized
    }
}

fun formatOneDecimal(value: Double): String {
    return String.format(Locale.JAPAN, "%.1f", value)
}
