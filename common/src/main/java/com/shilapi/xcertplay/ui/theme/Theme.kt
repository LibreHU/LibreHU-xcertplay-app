package com.shilapi.xcertplay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun XcertplayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // LibreHU fork: light / dark and accent of LibreHU Launcher when it is installed.
    val launcher by com.shilapi.xcertplay.librehu.LibreHuCarPlay.theme.collectAsState()
    launcher?.let { t ->
        val accent = androidx.compose.ui.graphics.Color(t.accent)
        val onAccent = if (t.dark) androidx.compose.ui.graphics.Color(0xFF202124) else androidx.compose.ui.graphics.Color.White
        val r = t.retro?.map { androidx.compose.ui.graphics.Color(it) }
        val scheme =
            when {
                // The launcher's retro palette.
                r != null ->
                    (if (t.dark) darkColorScheme() else lightColorScheme()).copy(
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
                t.dark -> darkColorScheme(primary = accent, secondary = accent, tertiary = accent, onPrimary = onAccent, onSecondary = onAccent)
                else -> lightColorScheme(primary = accent, secondary = accent, tertiary = accent, onPrimary = onAccent, onSecondary = onAccent)
            }
        MaterialTheme(
            colorScheme = scheme,
            // Segment display font when chosen in the launcher (LibreHU Seg14, DSEG14, SIL OFL 1.1).
            typography = if (t.segment) segmentTypography() else Typography,
            content = content
        )
        return
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

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
