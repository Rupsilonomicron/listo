package io.github.rupsilonomicron.listo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Android 12 未満で使う既定の緑系配色
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0AE),
    onPrimaryContainer = Color(0xFF002203),
    secondary = Color(0xFF53634E),
    secondaryContainer = Color(0xFFD6E8CD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD494),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB7F0AE),
    secondary = Color(0xFFBACCB2),
    secondaryContainer = Color(0xFF3C4B38),
)

/** 重要項目の強調色。壁紙連動の配色とは独立して常に赤系にする */
data class ImportantColors(val accent: Color, val container: Color)

private val LightImportant = ImportantColors(accent = Color(0xFFC62828), container = Color(0xFFFFEBEE))
private val DarkImportant = ImportantColors(accent = Color(0xFFFF8A80), container = Color(0xFF4A2326))

val LocalImportantColors = staticCompositionLocalOf { LightImportant }

@Composable
fun ListoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalImportantColors provides if (dark) DarkImportant else LightImportant) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
