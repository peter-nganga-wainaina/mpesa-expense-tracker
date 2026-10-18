package com.example.expensetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Headings in a serif to give totals and titles some warmth; body text stays in the system sans.
// The phone's built-in serif stands in for Fraunces until font files are bundled.
private val Headings = FontFamily.Serif
private val Defaults = Typography()

private fun TextStyle.asHeading() = copy(fontFamily = Headings, fontWeight = FontWeight.Bold)

val Typography = Typography(
    displaySmall = Defaults.displaySmall.asHeading(),
    headlineLarge = Defaults.headlineLarge.asHeading(),
    headlineMedium = Defaults.headlineMedium.asHeading(),
    headlineSmall = Defaults.headlineSmall.asHeading(),
    titleLarge = Defaults.titleLarge.asHeading(),
)
