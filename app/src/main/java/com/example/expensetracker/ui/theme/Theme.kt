package com.example.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Hero surfaces (the month total, selected tab) use primaryContainer: maroon in both themes.
private val MaroonAndLatte = lightColorScheme(
    primary = Maroon,
    onPrimary = Cream,
    primaryContainer = Maroon,
    onPrimaryContainer = Cream,
    secondary = Color(0xFF8B5A3C),
    onSecondary = Cream,
    secondaryContainer = Color(0xFFE6CFB8),
    onSecondaryContainer = Espresso,
    tertiary = Color(0xFF9A6A12),
    onTertiary = Cream,
    background = Latte,
    onBackground = Espresso,
    surface = Latte,
    onSurface = Espresso,
    surfaceVariant = Color(0xFFEADBC8),
    onSurfaceVariant = Color(0xFF6E4A36),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7EADC),
    surfaceContainer = Color(0xFFEEDCC8),
    surfaceContainerHigh = Color(0xFFF9EDE0),
    surfaceContainerHighest = Color(0xFFFFF9F1),
    outline = Color(0xFFA88670),
    outlineVariant = Color(0xFFE2CDB8),
)

private val CocoaNight = darkColorScheme(
    // Light rose rather than maroon for primary: text and icons in it must read on chocolate.
    primary = Color(0xFFF0A6B3),
    onPrimary = Color(0xFF4A0E19),
    primaryContainer = DeepMaroon,
    onPrimaryContainer = WarmCream,
    secondary = Color(0xFFE0BC94),
    onSecondary = Espresso,
    secondaryContainer = Color(0xFF5A3A28),
    onSecondaryContainer = WarmCream,
    tertiary = Color(0xFFF2B33D),
    onTertiary = Espresso,
    background = Chocolate,
    onBackground = WarmCream,
    surface = Chocolate,
    onSurface = WarmCream,
    surfaceVariant = Color(0xFF432A1D),
    onSurfaceVariant = Color(0xFFE6D2BF),
    surfaceContainerLowest = Color(0xFF22130C),
    surfaceContainerLow = Color(0xFF321E14),
    surfaceContainer = Color(0xFF3A2419),
    surfaceContainerHigh = Color(0xFF432A1D),
    surfaceContainerHighest = Color(0xFF4C3123),
    outline = Color(0xFF9C7E68),
    outlineVariant = Color(0xFF5A3F2F),
)

private val LocalCategoryColors = staticCompositionLocalOf { CategoryColorsLight }

/** Maroon & Latte or Cocoa Night. No dynamic (wallpaper) colour: the look is part of the app. */
@Composable
fun ExpenseTrackerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) CocoaNight else MaroonAndLatte,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalCategoryColors provides if (darkTheme) CategoryColorsDark else CategoryColorsLight,
            content = content,
        )
    }
}

/** The colour that stands for a category everywhere: chips, chart slices, legends. */
@Composable
@ReadOnlyComposable
fun categoryColor(categoryId: Long): Color {
    val palette = LocalCategoryColors.current
    return palette[categoryColorIndex(categoryId, palette.size)]
}
