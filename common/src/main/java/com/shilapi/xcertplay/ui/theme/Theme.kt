package com.shilapi.xcertplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** 4dp grid and layout tokens of the Lavender system. */
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s12 = 48.dp
    val marginCompact = 16.dp
    val measure = 600.dp
}

/** MD3 shape scale: the larger, the more important or interactive. */
object Radius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 28.dp
}

private val LavenderShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/** Phones use 375dp on the short edge as 1.0x; tablets and head units scale up proportionally. */
const val BASE_SHORTEST_WIDTH_DP = 375f
const val MIN_UI_SCALE = 0.85f
const val MAX_UI_SCALE = 2.2f

/**
 * Proportional UI scaling: the larger the screen, the larger text, spacing and padding.
 * The font scale is locked to 1 and follows the same factor, so head units with unusual
 * system font sizes cannot break the layout.
 */
@Composable
fun ProvideScaledDensity(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val current = LocalDensity.current
    val shortestWidthDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val scaleFactor = (shortestWidthDp / BASE_SHORTEST_WIDTH_DP).coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)
    val scaled = remember(current.density, scaleFactor) {
        Density(density = current.density * scaleFactor, fontScale = 1f)
    }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

/**
 * Lavender theme. Light and dark follow the system automatically; there is no dynamic colour so
 * the brand seed stays consistent on every device.
 */
@Composable
fun XcertplayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    scaled: Boolean = true,
    content: @Composable () -> Unit,
) {
    // LibreHU fork: light / dark, accent (or retro palette) and font of LibreHU Launcher when it is installed.
    val launcher by com.shilapi.xcertplay.librehu.LibreHuCarPlay.theme.collectAsState()
    val dark = launcher?.dark ?: darkTheme
    val colors = launcher?.let { librehuScheme(it) } ?: if (dark) LavenderDark else LavenderLight
    val typography = if (launcher?.segment == true) segmentTypography() else Typography
    val extended = if (dark) LavenderExtendedDark else LavenderExtendedLight
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = LavenderShapes) {
            if (scaled) ProvideScaledDensity(content) else content()
        }
    }
}

/** Shortcut for the success/warning roles. */
object Lavender {
    val extended: LavenderExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}

/** LibreHU fork: Lavender with the launcher's accent, or the launcher's retro palette. */
private fun librehuScheme(t: com.shilapi.xcertplay.librehu.LibreHuCarPlay.LauncherTheme): androidx.compose.material3.ColorScheme {
    val base = if (t.dark) LavenderDark else LavenderLight
    val r = t.retro?.map { androidx.compose.ui.graphics.Color(it) }
    if (r != null) {
        return base.copy(
            background = r[0],
            surface = r[1],
            surfaceVariant = r[2],
            surfaceContainer = r[1],
            surfaceContainerHigh = r[2],
            primary = r[3],
            secondary = r[3],
            tertiary = r[3],
            onPrimary = r[4],
            onSecondary = r[4],
            onBackground = r[5],
            onSurface = r[5],
            onSurfaceVariant = r[6],
            outline = r[6],
        )
    }
    val accent = androidx.compose.ui.graphics.Color(t.accent)
    val onAccent = if (t.dark) androidx.compose.ui.graphics.Color(0xFF202124) else androidx.compose.ui.graphics.Color.White
    return base.copy(primary = accent, secondary = accent, tertiary = accent, onPrimary = onAccent, onSecondary = onAccent)
}

/** LibreHU fork: segment display font when chosen in the launcher (LibreHU Seg14, DSEG14, SIL OFL 1.1). */
private fun segmentTypography(): androidx.compose.material3.Typography {
    val seg = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.shilapi.xcertplay.host.R.font.librehu_seg14))
    return Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontFamily = seg),
        displayMedium = Typography.displayMedium.copy(fontFamily = seg),
        displaySmall = Typography.displaySmall.copy(fontFamily = seg),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = seg),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = seg),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = seg),
        titleLarge = Typography.titleLarge.copy(fontFamily = seg),
        titleMedium = Typography.titleMedium.copy(fontFamily = seg),
        titleSmall = Typography.titleSmall.copy(fontFamily = seg),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = seg),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = seg),
        bodySmall = Typography.bodySmall.copy(fontFamily = seg),
        labelLarge = Typography.labelLarge.copy(fontFamily = seg),
        labelMedium = Typography.labelMedium.copy(fontFamily = seg),
        labelSmall = Typography.labelSmall.copy(fontFamily = seg),
    )
}
