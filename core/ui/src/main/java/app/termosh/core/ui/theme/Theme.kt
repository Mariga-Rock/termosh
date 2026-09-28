package app.termosh.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class TermoshThemeOption(val displayName: String) {
    TOKYO_NIGHT("Tokyo Night"),
    CATPPUCCIN_FRAPPE("Catppuccin Frappe"),
    GITHUB_LIGHT("GitHub Light"),
}

@Composable
fun TermoshTheme(
    theme: TermoshThemeOption = TermoshThemeOption.TOKYO_NIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        TermoshThemeOption.TOKYO_NIGHT -> darkColorScheme(
            primary = TokyoNightColors.Blue,
            onPrimary = TokyoNightColors.Background,
            secondary = TokyoNightColors.Purple,
            onSecondary = TokyoNightColors.Background,
            tertiary = TokyoNightColors.Cyan,
            background = TokyoNightColors.Background,
            onBackground = TokyoNightColors.TextPrimary,
            surface = TokyoNightColors.Surface,
            onSurface = TokyoNightColors.TextPrimary,
            surfaceVariant = TokyoNightColors.SurfaceVariant,
            onSurfaceVariant = TokyoNightColors.TextSecondary,
            outline = TokyoNightColors.Border,
            error = TokyoNightColors.Error,
            onError = TokyoNightColors.OnError,
        )
        TermoshThemeOption.CATPPUCCIN_FRAPPE -> darkColorScheme(
            primary = CatppuccinFrappeColors.Blue,
            onPrimary = CatppuccinFrappeColors.Base,
            secondary = CatppuccinFrappeColors.Mauve,
            onSecondary = CatppuccinFrappeColors.Base,
            tertiary = CatppuccinFrappeColors.Sapphire,
            background = CatppuccinFrappeColors.Base,
            onBackground = CatppuccinFrappeColors.Text,
            surface = CatppuccinFrappeColors.Mantle,
            onSurface = CatppuccinFrappeColors.Text,
            surfaceVariant = CatppuccinFrappeColors.Surface0,
            onSurfaceVariant = CatppuccinFrappeColors.Subtext1,
            outline = CatppuccinFrappeColors.Surface2,
            error = CatppuccinFrappeColors.Error,
            onError = CatppuccinFrappeColors.OnError,
        )
        TermoshThemeOption.GITHUB_LIGHT -> lightColorScheme(
            primary = GitHubLightColors.Accent,
            onPrimary = GitHubLightColors.BgDefault,
            secondary = GitHubLightColors.Done,
            onSecondary = GitHubLightColors.BgDefault,
            tertiary = GitHubLightColors.Success,
            background = GitHubLightColors.BgDefault,
            onBackground = GitHubLightColors.FgDefault,
            surface = GitHubLightColors.BgSubtle,
            onSurface = GitHubLightColors.FgDefault,
            surfaceVariant = GitHubLightColors.BgInset,
            onSurfaceVariant = GitHubLightColors.FgMuted,
            outline = GitHubLightColors.BorderDefault,
            error = GitHubLightColors.Danger,
            onError = GitHubLightColors.BgDefault,
        )
    }

    val isLight = theme == TermoshThemeOption.GITHUB_LIGHT

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    MaterialTheme(colorScheme = dark, content = content)
}
