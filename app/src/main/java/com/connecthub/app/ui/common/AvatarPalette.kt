package com.connecthub.app.ui.common

import android.graphics.Color

/**
 * Generates a WhatsApp-style initials avatar: a deterministic colour (so the
 * same contact always gets the same colour) plus their first initial.
 */
object AvatarPalette {

    private val PALETTE = listOf(
        "#0A4D8C", // ch_blue
        "#0B6E4F", // ch_green
        "#D4A017", // ch_gold
        "#8E44AD",
        "#C0392B",
        "#16A085",
        "#2980B9",
        "#D35400"
    )

    fun colorFor(name: String): Int {
        if (name.isBlank()) return Color.parseColor(PALETTE.first())
        val index = kotlin.math.abs(name.hashCode()) % PALETTE.size
        return Color.parseColor(PALETTE[index])
    }

    fun initialFor(name: String): String {
        return name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
}
