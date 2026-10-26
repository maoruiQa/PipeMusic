package org.schabi.newpipe.music.ui.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.ThemeConfig
import org.schabi.newpipe.music.data.ThemePresets

/**
 * Full screen theme editor with a hand drawn HSV picker. Custom canvas bars are used instead of
 * Material sliders because the sliders are a lot heavier to render and are not needed here.
 */
@Composable
fun ThemeSettingsScreen(
    config: ThemeConfig,
    onDismiss: () -> Unit,
    onChange: ((ThemeConfig) -> ThemeConfig) -> Unit
) {
    val initialSeed = config.customSeed ?: ColorSchemeFactory.seedOf(config)
    val hsv = remember(initialSeed) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialSeed, it) }
    }
    var hue by remember(initialSeed) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialSeed) { mutableFloatStateOf(hsv[1]) }
    var value by remember(initialSeed) { mutableFloatStateOf(hsv[2]) }
    val preview = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    var hexText by remember(initialSeed) {
        mutableStateOf(String.format("#%06X", 0xFFFFFF and preview))
    }

    fun syncHex() {
        hexText = String.format(
            "#%06X",
            0xFFFFFF and AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null
            ) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.pm_theme_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.pm_theme_screen_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.pm_theme_presets),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            ThemePresets.all.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { preset ->
                        val selected = config.customSeed == null && config.presetId == preset.id
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onChange { it.copy(presetId = preset.id, customSeed = null) }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(preset.gradientStart),
                                                Color(preset.gradientEnd)
                                            )
                                        )
                                    )
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            Color.White.copy(alpha = 0.15f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = stringResource(preset.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pm_theme_custom),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(preview)),
                    contentAlignment = Alignment.Center
                ) {
                    if (config.customSeed != null) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        parseHex(input)?.let { parsed ->
                            val converted = FloatArray(3)
                            AndroidColor.colorToHSV(parsed, converted)
                            hue = converted[0]
                            saturation = converted[1]
                            value = converted[2]
                        }
                    },
                    label = { Text(stringResource(R.string.pm_theme_hex)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.pm_theme_hue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradientBar(
                fraction = (hue / 360f).coerceIn(0f, 1f),
                colors = listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF00FFFF),
                    Color(0xFF0000FF),
                    Color(0xFFFF00FF),
                    Color(0xFFFF0000)
                )
            ) {
                hue = (it * 360f).coerceIn(0f, 360f)
                syncHex()
            }
            Text(
                text = stringResource(R.string.pm_theme_saturation),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradientBar(
                fraction = saturation.coerceIn(0f, 1f),
                colors = listOf(
                    Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0f, value))),
                    Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, value)))
                )
            ) {
                saturation = it.coerceIn(0f, 1f)
                syncHex()
            }
            Text(
                text = stringResource(R.string.pm_theme_brightness),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradientBar(
                fraction = ((value - 0.15f) / 0.85f).coerceIn(0f, 1f),
                colors = listOf(
                    Color.Black,
                    Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                )
            ) {
                value = (0.15f + it * 0.85f).coerceIn(0.15f, 1f)
                syncHex()
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    onChange { it.copy(customSeed = preview) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.pm_theme_apply_custom))
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.pm_theme_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeOption(
                    stringResource(R.string.pm_theme_mode_system),
                    config.darkMode == 0,
                    Modifier.weight(1f)
                ) {
                    onChange { it.copy(darkMode = 0) }
                }
                ModeOption(
                    stringResource(R.string.pm_theme_mode_light),
                    config.darkMode == 1,
                    Modifier.weight(1f)
                ) {
                    onChange { it.copy(darkMode = 1) }
                }
                ModeOption(
                    stringResource(R.string.pm_theme_mode_dark),
                    config.darkMode == 2,
                    Modifier.weight(1f)
                ) {
                    onChange { it.copy(darkMode = 2) }
                }
            }

            Spacer(Modifier.height(18.dp))
            ToggleRow(
                title = stringResource(R.string.pm_theme_amoled),
                description = stringResource(R.string.pm_theme_amoled_desc),
                checked = config.amoled,
                onCheckedChange = { enabled -> onChange { it.copy(amoled = enabled) } }
            )
            ToggleRow(
                title = stringResource(R.string.pm_theme_artwork_tint),
                description = stringResource(R.string.pm_theme_artwork_tint_desc),
                checked = config.artworkTint,
                onCheckedChange = { enabled -> onChange { it.copy(artworkTint = enabled) } }
            )
            ToggleRow(
                title = stringResource(R.string.pm_theme_dynamic),
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    stringResource(R.string.pm_theme_dynamic_desc)
                } else {
                    stringResource(R.string.pm_theme_dynamic_unavailable)
                },
                checked = config.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = { enabled -> onChange { it.copy(dynamicColor = enabled) } }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.pm_action_done))
            }
        }
    }
}

@Composable
private fun GradientBar(
    fraction: Float,
    colors: List<Color>,
    onChange: (Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val barHeight = 10.dp.toPx()
        val top = (size.height - barHeight) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            topLeft = Offset(0f, top),
            size = Size(size.width, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f)
        )
        val radius = 9.dp.toPx()
        val centerX = radius + fraction.coerceIn(0f, 1f) * (size.width - radius * 2)
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(centerX, size.height / 2f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = radius,
            center = Offset(centerX, size.height / 2f),
            style = Stroke(1.5.dp.toPx())
        )
    }
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

private fun parseHex(input: String): Int? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) {
        return null
    }
    return runCatching {
        val value = cleaned.toLong(16)
        if (cleaned.length == 6) {
            (0xFF000000L or value).toInt()
        } else {
            value.toInt()
        }
    }.getOrNull()
}
