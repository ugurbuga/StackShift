package com.ugurbuga.blockgames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.game.model.toBlockColorPalette
import com.ugurbuga.blockgames.settings.AppSettings

@Immutable
data class BlockGamesUiColors(
	val screenGradientTop: Color,
	val screenGradientMiddle: Color,
	val screenGradientBottom: Color,
	val gameSurface: Color,
	val settingsHeroStart: Color,
	val settingsHeroEnd: Color,
	val boardGradientTop: Color,
	val boardGradientBottom: Color,
	val panel: Color,
	val panelMuted: Color,
	val panelHighlight: Color,
	val panelStroke: Color,
	val actionButton: Color,
	val actionPrimary: Color,
	val actionSecondary: Color,
	val actionSuccess: Color,
	val actionWarning: Color,
	val actionDanger: Color,
	val actionButtonDisabled: Color,
	val actionIcon: Color,
	val actionIconDisabled: Color,
	val metricCard: Color,
	val chipPreview: Color,
	val chip: Color,
	val chipSelected: Color,
	val subtitle: Color,
	val success: Color,
	val warning: Color,
	val danger: Color,
	val launchTrack: Color,
	val launchGlow: Color,
	val launchAccent: Color,
	val guideAccent: Color,
	val dialogStart: Color,
	val dialogEnd: Color,
	val overlay: Color,
	val selectionStackShift: Color,
	val selectionBlockWise: Color,
	val selectionMergeShift: Color,
	val selectionBoomBlocks: Color,
	val boardGridLine: Color,
	val boardOutline: Color,
	val boardOutlineGlow: Color,
	val boardSignaturePrimary: Color,
	val boardSignatureSecondary: Color,
	val boardEmptyCell: Color,
	val boardEmptyCellBorder: Color,
)

@Immutable
data class BlockGamesThemeSpec(
	val colorScheme: ColorScheme,
	val uiColors: BlockGamesUiColors,
)

val LocalBlockGamesUiColors = staticCompositionLocalOf {
	blockGamesThemeSpec(
		palette = AppColorPalette.Classic,
		darkTheme = true,
	).uiColors
}

object BlockGamesThemeTokens {
	val uiColors: BlockGamesUiColors
		@Composable get() = LocalBlockGamesUiColors.current
}

@Composable
fun isBlockGamesDarkTheme(settings: AppSettings): Boolean = settings.themeMode.isDark ?: isSystemInDarkTheme()

fun appBackgroundBrush(uiColors: BlockGamesUiColors): Brush = Brush.verticalGradient(
    colors = listOf(
        uiColors.screenGradientTop,
        uiColors.screenGradientMiddle,
        uiColors.screenGradientBottom,
    ),
)

fun blockGamesThemeSpec(
	settings: AppSettings,
	darkTheme: Boolean,
): BlockGamesThemeSpec = blockGamesThemeSpec(
	palette = settings.themeColorPalette,
	darkTheme = darkTheme,
)

fun blockGamesThemeSpec(
	palette: AppColorPalette,
	darkTheme: Boolean,
): BlockGamesThemeSpec {
	val colorScheme = when (palette) {
		AppColorPalette.Classic -> classicColorScheme(darkTheme)
		AppColorPalette.Aurora -> auroraColorScheme(darkTheme)
		AppColorPalette.Sunset -> sunsetColorScheme(darkTheme)
		AppColorPalette.ModernNeon -> modernNeonColorScheme(darkTheme)
		AppColorPalette.SoftPastel -> softPastelColorScheme(darkTheme)
		AppColorPalette.MinimalMonochrome -> minimalMonochromeColorScheme(darkTheme)
	}
	val gradients = gradientStops(palette = palette, darkTheme = darkTheme)
	val feedback = feedbackColors(palette = palette, darkTheme = darkTheme)
	val boardSignature = boardSignatureColors(palette = palette, darkTheme = darkTheme)
	val guideAccent = guideAccentColor(palette = palette, darkTheme = darkTheme)
	val actionAccents = actionAccentColors(palette = palette, darkTheme = darkTheme)
	val selectionAccents = selectionAccentColors(palette = palette, darkTheme = darkTheme)

	return BlockGamesThemeSpec(
		colorScheme = colorScheme,
		uiColors = BlockGamesUiColors(
			screenGradientTop = gradients.screenTop,
			screenGradientMiddle = gradients.screenMiddle,
			screenGradientBottom = gradients.screenBottom,
			gameSurface = mixColors(
				first = gradients.boardTop,
				second = gradients.boardBottom,
				fraction = if (darkTheme) 0.34f else 0.28f,
			),
			settingsHeroStart = colorScheme.primaryContainer,
			settingsHeroEnd = colorScheme.secondaryContainer,
			boardGradientTop = gradients.boardTop,
			boardGradientBottom = gradients.boardBottom,
			panel = if (darkTheme) {
				colorScheme.surface.copy(alpha = 0.92f)
			} else {
				mixColors(
					first = colorScheme.surface,
					second = colorScheme.background,
					fraction = 0.10f,
				).copy(alpha = 0.99f)
			},
			panelMuted = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.76f else 0.98f),
			panelHighlight = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.82f else 1f),
			panelStroke = colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.66f else 0.98f),
			actionButton = colorScheme.primary.copy(alpha = if (darkTheme) 0.92f else 0.96f),
			actionPrimary = actionAccents.primary,
			actionSecondary = actionAccents.secondary,
			actionSuccess = actionAccents.success,
			actionWarning = actionAccents.warning,
			actionDanger = actionAccents.danger,
			actionButtonDisabled = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.42f else 0.82f),
			actionIcon = colorScheme.onPrimary,
			actionIconDisabled = colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
			metricCard = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.70f else 0.94f),
			chipPreview = colorScheme.surface.copy(alpha = if (darkTheme) 0.84f else 1f),
			chip = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.56f else 0.94f),
			chipSelected = colorScheme.primaryContainer.copy(alpha = if (darkTheme) 0.92f else 1f),
			subtitle = if (darkTheme) {
				colorScheme.onSurfaceVariant
			} else {
				mixColors(
					first = colorScheme.onSurfaceVariant,
					second = colorScheme.onSurface,
					fraction = 0.26f,
				)
			},
			success = feedback.success,
			warning = feedback.warning,
			danger = feedback.danger,
			launchTrack = if (darkTheme) {
				colorScheme.surface.copy(alpha = 0.48f)
			} else {
				colorScheme.surfaceVariant.copy(alpha = 0.98f)
			},
			launchGlow = colorScheme.primary.copy(alpha = if (darkTheme) 0.74f else 0.52f),
			launchAccent = colorScheme.tertiary,
			guideAccent = guideAccent,
			dialogStart = colorScheme.surface.copy(alpha = if (darkTheme) 0.98f else 1f),
			dialogEnd = colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.92f else 0.98f),
			overlay = if (darkTheme) Color(0xC4171E31) else Color(0xBDE7EEF9),
			selectionStackShift = selectionAccents.stackShift,
			selectionBlockWise = selectionAccents.blockWise,
			selectionMergeShift = selectionAccents.mergeShift,
			selectionBoomBlocks = selectionAccents.boomBlocks,
			boardGridLine = colorScheme.outlineVariant.copy(alpha = if (darkTheme) 0.52f else 0.62f),
			boardOutline = colorScheme.outline.copy(alpha = if (darkTheme) 0.80f else 0.78f),
			boardOutlineGlow = colorScheme.primary.copy(alpha = if (darkTheme) 0.30f else 0.24f),
			boardSignaturePrimary = boardSignature.primary,
			boardSignatureSecondary = boardSignature.secondary,
			boardEmptyCell = mixColors(
				first = gradients.boardTop,
				second = colorScheme.surfaceVariant,
				fraction = if (darkTheme) 0.28f else 0.40f,
			).copy(alpha = if (darkTheme) 0.60f else 0.92f),
			boardEmptyCellBorder = mixColors(
				first = gradients.boardBottom,
				second = colorScheme.outlineVariant,
				fraction = if (darkTheme) 0.54f else 0.50f,
			).copy(alpha = if (darkTheme) 0.70f else 0.76f),
		),
	)
}

private fun classicColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.Classic), darkTheme)

private fun auroraColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.Aurora), darkTheme)

private fun sunsetColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.Sunset), darkTheme)

private fun modernNeonColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.ModernNeon), darkTheme)

private fun softPastelColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.SoftPastel), darkTheme)

private fun minimalMonochromeColorScheme(darkTheme: Boolean): ColorScheme = seededColorScheme(themeSeed(AppColorPalette.MinimalMonochrome), darkTheme)

@Immutable
private data class ThemeSeed(
	val primary: Color,
	val secondary: Color,
	val tertiary: Color,
	val success: Color,
	val warning: Color,
	val danger: Color,
	val accent: Color,
	val styleStackShift: Color,
	val styleBlockWise: Color,
	val styleMergeShift: Color,
	val styleBoomBlocks: Color,
)

private fun themeSeed(palette: AppColorPalette): ThemeSeed {
	val blockPalette = palette.toBlockColorPalette()
	fun tone(tone: CellTone): Color = tone.paletteColor(blockPalette)

	return when (palette) {
		AppColorPalette.Classic -> ThemeSeed(
			primary = tone(CellTone.Cyan),
			secondary = tone(CellTone.Violet),
			tertiary = tone(CellTone.Gold),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Gold),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Blue),
			styleStackShift = tone(CellTone.Cyan),
			styleBlockWise = tone(CellTone.Amber),
			styleMergeShift = tone(CellTone.Violet),
			styleBoomBlocks = tone(CellTone.Coral),
		)
		AppColorPalette.Aurora -> ThemeSeed(
			primary = tone(CellTone.Cyan),
			secondary = tone(CellTone.Violet),
			tertiary = tone(CellTone.Amber),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Gold),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Blue),
			styleStackShift = tone(CellTone.Cyan),
			styleBlockWise = tone(CellTone.Emerald),
			styleMergeShift = tone(CellTone.Violet),
			styleBoomBlocks = tone(CellTone.Rose),
		)
		AppColorPalette.Sunset -> ThemeSeed(
			primary = tone(CellTone.Cyan),
			secondary = tone(CellTone.Blue),
			tertiary = tone(CellTone.Gold),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Amber),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Violet),
			styleStackShift = tone(CellTone.Cyan),
			styleBlockWise = tone(CellTone.Emerald),
			styleMergeShift = tone(CellTone.Violet),
			styleBoomBlocks = tone(CellTone.Coral),
		)
		AppColorPalette.ModernNeon -> ThemeSeed(
			primary = tone(CellTone.Cyan),
			secondary = tone(CellTone.Violet),
			tertiary = tone(CellTone.Lime),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Amber),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Blue),
			styleStackShift = tone(CellTone.Cyan),
			styleBlockWise = tone(CellTone.Lime),
			styleMergeShift = tone(CellTone.Violet),
			styleBoomBlocks = tone(CellTone.Rose),
		)
		AppColorPalette.SoftPastel -> ThemeSeed(
			primary = tone(CellTone.Cyan),
			secondary = tone(CellTone.Violet),
			tertiary = tone(CellTone.Gold),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Amber),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Blue),
			styleStackShift = tone(CellTone.Cyan),
			styleBlockWise = tone(CellTone.Gold),
			styleMergeShift = tone(CellTone.Violet),
			styleBoomBlocks = tone(CellTone.Coral),
		)
		AppColorPalette.MinimalMonochrome -> ThemeSeed(
			primary = tone(CellTone.Blue),
			secondary = tone(CellTone.Emerald),
			tertiary = tone(CellTone.Gold),
			success = tone(CellTone.Emerald),
			warning = tone(CellTone.Gold),
			danger = tone(CellTone.Coral),
			accent = tone(CellTone.Lime),
			styleStackShift = tone(CellTone.Blue),
			styleBlockWise = tone(CellTone.Lime),
			styleMergeShift = tone(CellTone.Emerald),
			styleBoomBlocks = tone(CellTone.Coral),
		)
	}
}

private fun seededColorScheme(
	seed: ThemeSeed,
	darkTheme: Boolean,
): ColorScheme {
	val darkText = mixColors(
		first = Color(0xFF172335),
		second = seed.accent,
		fraction = 0.08f,
	)
	return if (darkTheme) {
		val background = mixColors(seed.primary, Color(0xFF040814), 0.92f)
		val surface = mixColors(seed.accent, Color(0xFF0C1422), 0.84f)
		val surfaceVariant = mixColors(seed.secondary, Color(0xFF132033), 0.80f)
		val primary = lighten(seed.primary, 0.08f)
		val secondary = lighten(seed.secondary, 0.10f)
		val tertiary = lighten(seed.tertiary, 0.06f)
		darkColorScheme(
			primary = primary,
			onPrimary = contentColorFor(primary),
			primaryContainer = mixColors(seed.primary, surface, 0.66f),
			onPrimaryContainer = lighten(seed.primary, 0.74f),
			secondary = secondary,
			onSecondary = contentColorFor(secondary),
			secondaryContainer = mixColors(seed.secondary, surface, 0.70f),
			onSecondaryContainer = lighten(seed.secondary, 0.74f),
			tertiary = tertiary,
			onTertiary = contentColorFor(tertiary),
			tertiaryContainer = mixColors(seed.tertiary, surface, 0.72f),
			onTertiaryContainer = lighten(seed.tertiary, 0.78f),
			background = background,
			onBackground = Color(0xFFF4F8FF),
			surface = surface,
			onSurface = Color(0xFFF4F8FF),
			surfaceVariant = surfaceVariant,
			onSurfaceVariant = mixColors(Color(0xFFDCE6F3), seed.primary, 0.12f),
			outline = mixColors(Color(0xFF8EA1B7), seed.accent, 0.22f),
			outlineVariant = mixColors(Color(0xFF324154), seed.secondary, 0.18f),
		)
	} else {
		val background = mixColors(Color.White, seed.primary, 0.08f)
		val surface = mixColors(Color.White, seed.secondary, 0.03f)
		val surfaceVariant = mixColors(Color(0xFFF1F5F9), seed.secondary, 0.10f)
		val primary = darken(seed.primary, 0.18f)
		val secondary = darken(seed.secondary, 0.20f)
		val tertiary = darken(seed.tertiary, 0.24f)
		val primaryContainer = mixColors(Color.White, seed.primary, 0.18f)
		val secondaryContainer = mixColors(Color.White, seed.secondary, 0.16f)
		val tertiaryContainer = mixColors(Color.White, seed.tertiary, 0.18f)
		lightColorScheme(
			primary = primary,
			onPrimary = contentColorFor(primary),
			primaryContainer = primaryContainer,
			onPrimaryContainer = contentColorFor(primaryContainer),
			secondary = secondary,
			onSecondary = contentColorFor(secondary),
			secondaryContainer = secondaryContainer,
			onSecondaryContainer = contentColorFor(secondaryContainer),
			tertiary = tertiary,
			onTertiary = contentColorFor(tertiary),
			tertiaryContainer = tertiaryContainer,
			onTertiaryContainer = contentColorFor(tertiaryContainer),
			background = background,
			onBackground = darkText,
			surface = surface,
			onSurface = darkText,
			surfaceVariant = surfaceVariant,
			onSurfaceVariant = mixColors(Color(0xFF566B81), seed.accent, 0.10f),
			outline = mixColors(Color(0xFF7F93A8), seed.accent, 0.22f),
			outlineVariant = mixColors(Color(0xFFD8E1EB), seed.primary, 0.14f),
		)
	}
}

@Immutable
private data class GradientStops(
	val screenTop: Color,
	val screenMiddle: Color,
	val screenBottom: Color,
	val boardTop: Color,
	val boardBottom: Color,
)

private fun gradientStopsForSeed(
	seed: ThemeSeed,
	darkTheme: Boolean,
): GradientStops = if (darkTheme) {
	GradientStops(
		screenTop = mixColors(Color(0xFF06111A), seed.primary, 0.26f),
		screenMiddle = mixColors(Color(0xFF07111B), seed.secondary, 0.16f),
		screenBottom = mixColors(Color(0xFF04070F), seed.accent, 0.20f),
		boardTop = mixColors(Color(0xFF09131D), seed.primary, 0.18f),
		boardBottom = mixColors(Color(0xFF07101A), seed.secondary, 0.16f),
	)
} else {
	GradientStops(
		screenTop = mixColors(Color.White, seed.primary, 0.12f),
		screenMiddle = mixColors(Color.White, seed.secondary, 0.07f),
		screenBottom = mixColors(Color.White, seed.tertiary, 0.03f),
		boardTop = mixColors(Color(0xFFFCFEFF), seed.primary, 0.07f),
		boardBottom = mixColors(Color(0xFFF6FAFF), seed.accent, 0.10f),
	)
}

@Immutable
private data class FeedbackColors(
	val success: Color,
	val warning: Color,
	val danger: Color,
)

private fun gradientStops(
	palette: AppColorPalette,
	darkTheme: Boolean,
): GradientStops = gradientStopsForSeed(
	seed = themeSeed(palette),
	darkTheme = darkTheme,
)

private fun feedbackColorsForSeed(
	seed: ThemeSeed,
	darkTheme: Boolean,
): FeedbackColors = FeedbackColors(
	success = if (darkTheme) lighten(seed.success, 0.06f) else darken(seed.success, 0.18f),
	warning = if (darkTheme) lighten(seed.warning, 0.04f) else darken(seed.warning, 0.22f),
	danger = if (darkTheme) lighten(seed.danger, 0.04f) else darken(seed.danger, 0.18f),
)

private fun feedbackColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): FeedbackColors = feedbackColorsForSeed(
	seed = themeSeed(palette),
	darkTheme = darkTheme,
)

private fun guideAccentColor(
	palette: AppColorPalette,
	darkTheme: Boolean,
): Color {
	val seed = themeSeed(palette)
	return if (darkTheme) lighten(seed.accent, 0.08f) else darken(seed.accent, 0.12f)
}

@Immutable
private data class ActionAccentColors(
	val primary: Color,
	val secondary: Color,
	val success: Color,
	val warning: Color,
	val danger: Color,
)

private fun actionAccentColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): ActionAccentColors {
	val seed = themeSeed(palette)
	return ActionAccentColors(
		primary = if (darkTheme) lighten(seed.primary, 0.08f) else darken(seed.primary, 0.10f),
		secondary = if (darkTheme) lighten(seed.secondary, 0.08f) else darken(seed.secondary, 0.10f),
		success = if (darkTheme) lighten(seed.success, 0.06f) else darken(seed.success, 0.14f),
		warning = if (darkTheme) lighten(seed.warning, 0.04f) else darken(seed.warning, 0.16f),
		danger = if (darkTheme) lighten(seed.danger, 0.04f) else darken(seed.danger, 0.14f),
	)
}

@Immutable
private data class SelectionAccentColors(
	val stackShift: Color,
	val blockWise: Color,
	val mergeShift: Color,
	val boomBlocks: Color,
)

private fun selectionAccentColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): SelectionAccentColors {
	val seed = themeSeed(palette)
	fun normalize(color: Color): Color = if (darkTheme) lighten(color, 0.08f) else darken(color, 0.12f)
	return SelectionAccentColors(
		stackShift = normalize(seed.styleStackShift),
		blockWise = normalize(seed.styleBlockWise),
		mergeShift = normalize(seed.styleMergeShift),
		boomBlocks = normalize(seed.styleBoomBlocks),
	)
}

@Immutable
private data class BoardSignatureColors(
	val primary: Color,
	val secondary: Color,
)

private fun boardSignatureColorsForSeed(
	seed: ThemeSeed,
	darkTheme: Boolean,
): BoardSignatureColors = BoardSignatureColors(
	primary = if (darkTheme) seed.primary.copy(alpha = 0.20f) else seed.primary.copy(alpha = 0.16f),
	secondary = if (darkTheme) seed.accent.copy(alpha = 0.14f) else seed.accent.copy(alpha = 0.10f),
)

private fun boardSignatureColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): BoardSignatureColors = boardSignatureColorsForSeed(
	seed = themeSeed(palette),
	darkTheme = darkTheme,
)

private fun mixColors(
	first: Color,
	second: Color,
	fraction: Float,
): Color {
	val t = fraction.coerceIn(0f, 1f)
	return Color(
		red = first.red + ((second.red - first.red) * t),
		green = first.green + ((second.green - first.green) * t),
		blue = first.blue + ((second.blue - first.blue) * t),
		alpha = first.alpha + ((second.alpha - first.alpha) * t),
	)
}

private fun lighten(
	color: Color,
	fraction: Float,
): Color = mixColors(color, Color.White, fraction)

private fun darken(
	color: Color,
	fraction: Float,
): Color = mixColors(color, Color.Black, fraction)

private fun contentColorFor(background: Color): Color =
	if (background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f > 0.62f) {
		Color(0xFF08111F)
	} else {
		Color.White
	}
