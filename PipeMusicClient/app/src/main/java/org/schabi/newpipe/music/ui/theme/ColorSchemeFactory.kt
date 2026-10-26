package org.schabi.newpipe.music.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.google.android.material.color.utilities.Scheme
import org.schabi.newpipe.music.data.ThemeConfig
import org.schabi.newpipe.music.data.ThemePresets

object ColorSchemeFactory {

    fun seedOf(config: ThemeConfig): Int {
        config.customSeed?.let { return it }
        return ThemePresets.find(config.presetId).seed
    }

    fun build(
        context: Context,
        config: ThemeConfig,
        dark: Boolean,
        useDynamic: Boolean
    ): ColorScheme {
        if (useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamic = if (dark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            return if (config.amoled && dark) {
                dynamic.withAmoled()
            } else {
                dynamic
            }
        }
        val seed = seedOf(config)
        val scheme = if (dark) Scheme.dark(seed) else Scheme.light(seed)
        return scheme.toColorScheme(dark, config.amoled)
    }

    private fun Scheme.toColorScheme(dark: Boolean, amoled: Boolean): ColorScheme {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        val surfaceTone = Color(surface)
        val onSurfaceTone = Color(onSurface)
        val background = if (amoled && dark) Color.Black else Color(background)
        val surfaceColor = if (amoled && dark) Color.Black else surfaceTone
        val containerLowest = if (amoled && dark) {
            Color(0xFF050608)
        } else {
            lerp(surfaceColor, if (dark) Color.Black else Color.White, 0.02f)
        }
        val containerLow = lerp(surfaceColor, if (dark) Color.White else Color.Black, 0.05f)
        val container = lerp(surfaceColor, if (dark) Color.White else Color.Black, 0.08f)
        val containerHigh = lerp(surfaceColor, if (dark) Color.White else Color.Black, 0.11f)
        val containerHighest = lerp(surfaceColor, if (dark) Color.White else Color.Black, 0.14f)

        return base.copy(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            secondary = Color(secondary),
            onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(tertiary),
            onTertiary = Color(onTertiary),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(onTertiaryContainer),
            error = Color(error),
            onError = Color(onError),
            errorContainer = Color(errorContainer),
            onErrorContainer = Color(onErrorContainer),
            background = background,
            onBackground = Color(onBackground),
            surface = surfaceColor,
            onSurface = onSurfaceTone,
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
            outline = Color(outline),
            outlineVariant = Color(outlineVariant),
            scrim = Color(scrim),
            inverseSurface = Color(inverseSurface),
            inverseOnSurface = Color(inverseOnSurface),
            inversePrimary = Color(inversePrimary),
            surfaceTint = Color(primary),
            surfaceBright = lerp(surfaceColor, Color.White, if (dark) 0.16f else 0.04f),
            surfaceDim = lerp(surfaceColor, Color.Black, if (dark) 0.12f else 0.02f),
            surfaceContainerLowest = containerLowest,
            surfaceContainerLow = containerLow,
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
            surfaceContainerHighest = containerHighest
        )
    }

    private fun ColorScheme.withAmoled(): ColorScheme = copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color(0xFF050608),
        surfaceContainerLow = lerp(Color.Black, Color.White, 0.05f),
        surfaceContainer = lerp(Color.Black, Color.White, 0.08f),
        surfaceContainerHigh = lerp(Color.Black, Color.White, 0.11f),
        surfaceContainerHighest = lerp(Color.Black, Color.White, 0.14f)
    )

    fun gradientColors(config: ThemeConfig): List<Color> {
        val custom = config.customSeed
        if (custom != null) {
            val start = Color(custom)
            val end = lerp(start, Color(0xFFFF7A6B), 0.45f)
            return listOf(start, end)
        }
        val preset = ThemePresets.find(config.presetId)
        return listOf(Color(preset.gradientStart), Color(preset.gradientEnd))
    }
}
