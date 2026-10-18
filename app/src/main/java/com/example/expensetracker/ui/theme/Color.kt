package com.example.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Maroon & Latte (light): latte ground, espresso text, maroon for the moments that matter.
internal val Maroon = Color(0xFF7A1C2B)
internal val Latte = Color(0xFFF4E6D6)
internal val Espresso = Color(0xFF3B1F14)
internal val Cream = Color(0xFFFFF4EA)

// Cocoa Night (dark): chocolate ground, cream text, a deeper maroon for highlights.
internal val DeepMaroon = Color(0xFF8E2236)
internal val Chocolate = Color(0xFF2A1810)
internal val WarmCream = Color(0xFFF8EADB)

/**
 * Category colours: vibrant warm accents, separate from maroon and brown so a chart's slices stay
 * distinguishable. Neighbouring entries differ in lightness as well as hue. The dark set is a touch
 * brighter so it holds up on chocolate.
 */
internal val CategoryColorsLight = listOf(
    Color(0xFFF2B33D), // gold
    Color(0xFFF08AA0), // rose
    Color(0xFFA9CF8E), // sage
    Color(0xFFE8774F), // terracotta
    Color(0xFFD9B48F), // caramel
    Color(0xFF8FC9C0), // teal
    Color(0xFFC9A2E8), // lilac
    Color(0xFFF5D06B), // butter
)

internal val CategoryColorsDark = listOf(
    Color(0xFFF2B33D),
    Color(0xFFF58EA6),
    Color(0xFFA9D18E),
    Color(0xFFEE8456),
    Color(0xFFE0BC94),
    Color(0xFF9AD4CB),
    Color(0xFFD2B0F0),
    Color(0xFFF7DA7A),
)
