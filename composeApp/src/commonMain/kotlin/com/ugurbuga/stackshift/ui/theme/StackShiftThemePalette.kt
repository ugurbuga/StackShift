package com.ugurbuga.stackshift.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.settings.AppSettings

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

private fun classicColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFF7BE4B0),
		onPrimary = Color(0xFF072114),
		primaryContainer = Color(0xFF123A29),
		onPrimaryContainer = Color(0xFFD6FFE7),
		secondary = Color(0xFF9F92FF),
		onSecondary = Color(0xFF211847),
		secondaryContainer = Color(0xFF32245E),
		onSecondaryContainer = Color(0xFFE7E1FF),
		tertiary = Color(0xFFFFCF6B),
		onTertiary = Color(0xFF2C1B00),
		tertiaryContainer = Color(0xFF4D3710),
		onTertiaryContainer = Color(0xFFFFE8B5),
		background = Color(0xFF071018),
		onBackground = Color(0xFFF1F7FF),
		surface = Color(0xFF101A26),
		onSurface = Color(0xFFF1F7FF),
		surfaceVariant = Color(0xFF1A2635),
		onSurfaceVariant = Color(0xFFC6D3E1),
		outline = Color(0xFF8598AA),
		outlineVariant = Color(0xFF344352),
	)
} else {
	lightColorScheme(
		primary = Color(0xFF0EAAA0),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFDBF7F4),
		onPrimaryContainer = Color(0xFF002623),
		secondary = Color(0xFF8276EE),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFF0EBFF),
		onSecondaryContainer = Color(0xFF251C52),
		tertiary = Color(0xFFB98510),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFFFEECB),
		onTertiaryContainer = Color(0xFF352200),
		background = Color(0xFFF5F8FC),
		onBackground = Color(0xFF1C2D3E),
		surface = Color(0xFFFFFFFF),
		onSurface = Color(0xFF1C2D3E),
		surfaceVariant = Color(0xFFE5EDF6),
		onSurfaceVariant = Color(0xFF566A7D),
		outline = Color(0xFF8EA6BD),
		outlineVariant = Color(0xFFD4E1ED),
	)
}

private fun auroraColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFF68D9F0),
		onPrimary = Color(0xFF002730),
		primaryContainer = Color(0xFF0B4150),
		onPrimaryContainer = Color(0xFFC4F5FF),
		secondary = Color(0xFFB2A8FF),
		onSecondary = Color(0xFF251B50),
		secondaryContainer = Color(0xFF3A2D71),
		onSecondaryContainer = Color(0xFFE8E0FF),
		tertiary = Color(0xFF64E1BC),
		onTertiary = Color(0xFF002116),
		tertiaryContainer = Color(0xFF153C31),
		onTertiaryContainer = Color(0xFFC9F8E6),
		background = Color(0xFF07111A),
		onBackground = Color(0xFFF0F7FF),
		surface = Color(0xFF0E1927),
		onSurface = Color(0xFFF0F7FF),
		surfaceVariant = Color(0xFF17273A),
		onSurfaceVariant = Color(0xFFC2D4E6),
		outline = Color(0xFF8095A9),
		outlineVariant = Color(0xFF324558),
	)
} else {
	lightColorScheme(
		primary = Color(0xFF006F86),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFD9F5FB),
		onPrimaryContainer = Color(0xFF001F27),
		secondary = Color(0xFF6158D4),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFE9E7FF),
		onSecondaryContainer = Color(0xFF1C1544),
		tertiary = Color(0xFF118D6D),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFD6FAEE),
		onTertiaryContainer = Color(0xFF002117),
		background = Color(0xFFF2F8FE),
		onBackground = Color(0xFF132432),
		surface = Color(0xFFFCFEFF),
		onSurface = Color(0xFF132432),
		surfaceVariant = Color(0xFFE4EEF7),
		onSurfaceVariant = Color(0xFF435867),
		outline = Color(0xFF6B8396),
		outlineVariant = Color(0xFFCEDAE5),
	)
}

private fun sunsetColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFFFF9B72),
		onPrimary = Color(0xFF3C1400),
		primaryContainer = Color(0xFF5A2810),
		onPrimaryContainer = Color(0xFFFFDDCF),
		secondary = Color(0xFFC3A5FF),
		onSecondary = Color(0xFF2F1659),
		secondaryContainer = Color(0xFF442573),
		onSecondaryContainer = Color(0xFFF0E0FF),
		tertiary = Color(0xFFFFC85A),
		onTertiary = Color(0xFF301900),
		tertiaryContainer = Color(0xFF4D3307),
		onTertiaryContainer = Color(0xFFFFE5AD),
		background = Color(0xFF160D17),
		onBackground = Color(0xFFFFF5FA),
		surface = Color(0xFF211421),
		onSurface = Color(0xFFFFF5FA),
		surfaceVariant = Color(0xFF342334),
		onSurfaceVariant = Color(0xFFE1CADC),
		outline = Color(0xFF9E8798),
		outlineVariant = Color(0xFF564255),
	)
} else {
	lightColorScheme(
		primary = Color(0xFFC75D39),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFFFE3DA),
		onPrimaryContainer = Color(0xFF3B0F00),
		secondary = Color(0xFF7F59D1),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFF1E4FF),
		onSecondaryContainer = Color(0xFF271244),
		tertiary = Color(0xFFBB7004),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFFFE8C7),
		onTertiaryContainer = Color(0xFF341A00),
		background = Color(0xFFFFF7F4),
		onBackground = Color(0xFF2C2130),
		surface = Color(0xFFFFFEFD),
		onSurface = Color(0xFF2C2130),
		surfaceVariant = Color(0xFFF6EAEF),
		onSurfaceVariant = Color(0xFF675861),
		outline = Color(0xFF8D7783),
		outlineVariant = Color(0xFFE3D2D9),
	)
}

private fun modernNeonColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFF16F2FF),
		onPrimary = Color(0xFF00262B),
		primaryContainer = Color(0xFF004752),
		onPrimaryContainer = Color(0xFFCFFBFF),
		secondary = Color(0xFFFF3CC7),
		onSecondary = Color(0xFF39002F),
		secondaryContainer = Color(0xFF5E0D4D),
		onSecondaryContainer = Color(0xFFFFD7F5),
		tertiary = Color(0xFFB4FF1A),
		onTertiary = Color(0xFF163000),
		tertiaryContainer = Color(0xFF315500),
		onTertiaryContainer = Color(0xFFE7FFBF),
		background = Color(0xFF030611),
		onBackground = Color(0xFFF4F7FF),
		surface = Color(0xFF0B1120),
		onSurface = Color(0xFFF4F7FF),
		surfaceVariant = Color(0xFF18243B),
		onSurfaceVariant = Color(0xFFC8D3E6),
		outline = Color(0xFF8392AD),
		outlineVariant = Color(0xFF33415A),
	)
} else {
	lightColorScheme(
		primary = Color(0xFF007C96),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFD4FAFF),
		onPrimaryContainer = Color(0xFF001F26),
		secondary = Color(0xFFC2009B),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFFFD5F5),
		onSecondaryContainer = Color(0xFF3B002E),
		tertiary = Color(0xFF4E8E00),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFDEFFB6),
		onTertiaryContainer = Color(0xFF112300),
		background = Color(0xFFF1F7FF),
		onBackground = Color(0xFF172335),
		surface = Color(0xFFFBFDFF),
		onSurface = Color(0xFF172335),
		surfaceVariant = Color(0xFFE7EEF9),
		onSurfaceVariant = Color(0xFF495C75),
		outline = Color(0xFF657D9C),
		outlineVariant = Color(0xFFC8D8EA),
	)
}

private fun softPastelColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFFF5B9CC),
		onPrimary = Color(0xFF4B1726),
		primaryContainer = Color(0xFF69414E),
		onPrimaryContainer = Color(0xFFFFDEE8),
		secondary = Color(0xFFC8C4F8),
		onSecondary = Color(0xFF2F2C57),
		secondaryContainer = Color(0xFF484472),
		onSecondaryContainer = Color(0xFFE7E4FF),
		tertiary = Color(0xFFB7E6D2),
		onTertiary = Color(0xFF14382C),
		tertiaryContainer = Color(0xFF365649),
		onTertiaryContainer = Color(0xFFD6F6E8),
		background = Color(0xFF17151D),
		onBackground = Color(0xFFFFF7FB),
		surface = Color(0xFF211F28),
		onSurface = Color(0xFFFFF7FB),
		surfaceVariant = Color(0xFF35323D),
		onSurfaceVariant = Color(0xFFE2D8E6),
		outline = Color(0xFFA395A6),
		outlineVariant = Color(0xFF564F60),
	)
} else {
	lightColorScheme(
		primary = Color(0xFFD78CA8),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFFFE6EE),
		onPrimaryContainer = Color(0xFF3D0C1C),
		secondary = Color(0xFF8D96D8),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFEAEAFE),
		onSecondaryContainer = Color(0xFF15204A),
		tertiary = Color(0xFF7EBFA8),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFDEF6EC),
		onTertiaryContainer = Color(0xFF002118),
		background = Color(0xFFFFF7FA),
		onBackground = Color(0xFF2B2430),
		surface = Color(0xFFFFFCFE),
		onSurface = Color(0xFF2B2430),
		surfaceVariant = Color(0xFFF5EEF4),
		onSurfaceVariant = Color(0xFF635865),
		outline = Color(0xFF8B7D8A),
		outlineVariant = Color(0xFFE2D6E0),
	)
}

private fun minimalMonochromeColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
	darkColorScheme(
		primary = Color(0xFFB9C0C8),
		onPrimary = Color(0xFF20252B),
		primaryContainer = Color(0xFF383F47),
		onPrimaryContainer = Color(0xFFE1E7EF),
		secondary = Color(0xFFA8B0B9),
		onSecondary = Color(0xFF23282E),
		secondaryContainer = Color(0xFF31373E),
		onSecondaryContainer = Color(0xFFD7DEE7),
		tertiary = Color(0xFF8F99A3),
		onTertiary = Color(0xFF1D2329),
		tertiaryContainer = Color(0xFF2C333A),
		onTertiaryContainer = Color(0xFFD7DEE7),
		background = Color(0xFF0E1115),
		onBackground = Color(0xFFF5F7FA),
		surface = Color(0xFF171B20),
		onSurface = Color(0xFFF5F7FA),
		surfaceVariant = Color(0xFF252B32),
		onSurfaceVariant = Color(0xFFD1D7DE),
		outline = Color(0xFF8C959E),
		outlineVariant = Color(0xFF3B434C),
	)
} else {
	lightColorScheme(
		primary = Color(0xFF646C75),
		onPrimary = Color.White,
		primaryContainer = Color(0xFFE6EBEF),
		onPrimaryContainer = Color(0xFF1D2329),
		secondary = Color(0xFF727B85),
		onSecondary = Color.White,
		secondaryContainer = Color(0xFFE2E7EB),
		onSecondaryContainer = Color(0xFF172029),
		tertiary = Color(0xFF858E98),
		onTertiary = Color.White,
		tertiaryContainer = Color(0xFFE8ECEF),
		onTertiaryContainer = Color(0xFF1D2329),
		background = Color(0xFFF5F6F8),
		onBackground = Color(0xFF1A1F24),
		surface = Color(0xFFFCFCFD),
		onSurface = Color(0xFF1A1F24),
		surfaceVariant = Color(0xFFE9ECEF),
		onSurfaceVariant = Color(0xFF59636E),
		outline = Color(0xFF7B858F),
		outlineVariant = Color(0xFFD6DADE),
	)
}

@Immutable
private data class GradientStops(
	val screenTop: Color,
	val screenMiddle: Color,
	val screenBottom: Color,
	val boardTop: Color,
	val boardBottom: Color,
)

@Immutable
private data class FeedbackColors(
	val success: Color,
	val warning: Color,
	val danger: Color,
)

private fun gradientStops(
	palette: AppColorPalette,
	darkTheme: Boolean,
): GradientStops = when (palette) {
	AppColorPalette.Classic -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF152434),
			screenMiddle = Color(0xFF0C1621),
			screenBottom = Color(0xFF070D14),
			boardTop = Color(0xFF0D1621),
			boardBottom = Color(0xFF0B121A),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFE6F1FA),
			screenMiddle = Color(0xFFF3F7FC),
			screenBottom = Color(0xFFFFFFFF),
			boardTop = Color(0xFFF1F6FB),
			boardBottom = Color(0xFFEAF1F8),
		)
	}
	AppColorPalette.Aurora -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF172A43),
			screenMiddle = Color(0xFF0C1727),
			screenBottom = Color(0xFF070D15),
			boardTop = Color(0xFF0D1724),
			boardBottom = Color(0xFF0A121A),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFE7F5FD),
			screenMiddle = Color(0xFFF2F8FD),
			screenBottom = Color(0xFFFFFFFF),
			boardTop = Color(0xFFF0F7FC),
			boardBottom = Color(0xFFE7F1F8),
		)
	}
	AppColorPalette.Sunset -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF301C30),
			screenMiddle = Color(0xFF1A111E),
			screenBottom = Color(0xFF0F0912),
			boardTop = Color(0xFF1C131E),
			boardBottom = Color(0xFF130D14),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFFFF1EB),
			screenMiddle = Color(0xFFFFF7F3),
			screenBottom = Color(0xFFFFFEFD),
			boardTop = Color(0xFFFFF5F1),
			boardBottom = Color(0xFFF7EDF1),
		)
	}
	AppColorPalette.ModernNeon -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF151A47),
			screenMiddle = Color(0xFF090F25),
			screenBottom = Color(0xFF040813),
			boardTop = Color(0xFF0B1230),
			boardBottom = Color(0xFF080E1D),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFE9F4FF),
			screenMiddle = Color(0xFFF5FAFF),
			screenBottom = Color(0xFFFFFFFF),
			boardTop = Color(0xFFF0F7FF),
			boardBottom = Color(0xFFE8EEF8),
		)
	}
	AppColorPalette.SoftPastel -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF2C2432),
			screenMiddle = Color(0xFF1A1620),
			screenBottom = Color(0xFF110F15),
			boardTop = Color(0xFF221C27),
			boardBottom = Color(0xFF19151D),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFFFF1F6),
			screenMiddle = Color(0xFFFFF8FB),
			screenBottom = Color(0xFFFFFEFF),
			boardTop = Color(0xFFFFF5F9),
			boardBottom = Color(0xFFF4EDF4),
		)
	}
	AppColorPalette.MinimalMonochrome -> if (darkTheme) {
		GradientStops(
			screenTop = Color(0xFF262B31),
			screenMiddle = Color(0xFF171B20),
			screenBottom = Color(0xFF101317),
			boardTop = Color(0xFF181D22),
			boardBottom = Color(0xFF12161A),
		)
	} else {
		GradientStops(
			screenTop = Color(0xFFF0F2F4),
			screenMiddle = Color(0xFFF5F6F8),
			screenBottom = Color(0xFFFCFCFD),
			boardTop = Color(0xFFF4F5F7),
			boardBottom = Color(0xFFE9EBEE),
		)
	}
}

private fun feedbackColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): FeedbackColors = when (palette) {
	AppColorPalette.Classic -> FeedbackColors(
		success = if (darkTheme) Color(0xFF7BE4B0) else Color(0xFF1D8A63),
		warning = if (darkTheme) Color(0xFFFFCF6B) else Color(0xFFB46C00),
		danger = if (darkTheme) Color(0xFFFF7F8D) else Color(0xFFBC344A),
	)
	AppColorPalette.Aurora -> FeedbackColors(
		success = if (darkTheme) Color(0xFF64E1BC) else Color(0xFF197F62),
		warning = if (darkTheme) Color(0xFFFFD57A) else Color(0xFF986800),
		danger = if (darkTheme) Color(0xFFFF7DA9) else Color(0xFFB52F6A),
	)
	AppColorPalette.Sunset -> FeedbackColors(
		success = if (darkTheme) Color(0xFFFFC85A) else Color(0xFF8D6B00),
		warning = if (darkTheme) Color(0xFFFFAA7C) else Color(0xFFB44E1E),
		danger = if (darkTheme) Color(0xFFFF7B9D) else Color(0xFFBD355B),
	)
	AppColorPalette.ModernNeon -> FeedbackColors(
		success = if (darkTheme) Color(0xFF97FF2F) else Color(0xFF3D8600),
		warning = if (darkTheme) Color(0xFFFFC94C) else Color(0xFF9A6200),
		danger = if (darkTheme) Color(0xFFFF6EA9) else Color(0xFFB20074),
	)
	AppColorPalette.SoftPastel -> FeedbackColors(
		success = if (darkTheme) Color(0xFFAFE5CC) else Color(0xFF5C9C82),
		warning = if (darkTheme) Color(0xFFF7D5A3) else Color(0xFFAA7A3D),
		danger = if (darkTheme) Color(0xFFF2A6BD) else Color(0xFFC46B89),
	)
	AppColorPalette.MinimalMonochrome -> FeedbackColors(
		success = if (darkTheme) Color(0xFF9FCEBA) else Color(0xFF6D9080),
		warning = if (darkTheme) Color(0xFFD5C089) else Color(0xFF97845A),
		danger = if (darkTheme) Color(0xFFD6A2A2) else Color(0xFFA67575),
	)
}

private fun guideAccentColor(
	palette: AppColorPalette,
	darkTheme: Boolean,
): Color = when (palette) {
	AppColorPalette.Classic -> if (darkTheme) Color(0xFF7D8FFF) else Color(0xFF5768E8)
	AppColorPalette.Aurora -> if (darkTheme) Color(0xFF8A90FF) else Color(0xFF5C63E2)
	AppColorPalette.Sunset -> if (darkTheme) Color(0xFFD59CFF) else Color(0xFF9462D6)
	AppColorPalette.ModernNeon -> if (darkTheme) Color(0xFF8D63FF) else Color(0xFF6246FF)
	AppColorPalette.SoftPastel -> if (darkTheme) Color(0xFFC4B3FF) else Color(0xFF8D7FE0)
	AppColorPalette.MinimalMonochrome -> if (darkTheme) Color(0xFF8FA5BF) else Color(0xFF73859A)
}

@Immutable
private data class BoardSignatureColors(
	val primary: Color,
	val secondary: Color,
)

private fun boardSignatureColors(
	palette: AppColorPalette,
	darkTheme: Boolean,
): BoardSignatureColors = when (palette) {
	AppColorPalette.Classic -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x3348D8C8) else Color(0x3323D5C9),
		secondary = if (darkTheme) Color(0x226B74FF) else Color(0x228883F0),
	)
	AppColorPalette.Aurora -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x3339D5FF) else Color(0x3342C4FF),
		secondary = if (darkTheme) Color(0x223FDCA8) else Color(0x2244D9B0),
	)
	AppColorPalette.Sunset -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x33FF8C73) else Color(0x33FF9D7C),
		secondary = if (darkTheme) Color(0x22D48BFF) else Color(0x22D18DFF),
	)
	AppColorPalette.ModernNeon -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x3300E7FF) else Color(0x3300B7E5),
		secondary = if (darkTheme) Color(0x22FF4FD8) else Color(0x22D600A4),
	)
	AppColorPalette.SoftPastel -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x33FFB5C8) else Color(0x33F2AFC3),
		secondary = if (darkTheme) Color(0x22B8C9FF) else Color(0x229DB2F4),
	)
	AppColorPalette.MinimalMonochrome -> BoardSignatureColors(
		primary = if (darkTheme) Color(0x337FE3FF) else Color(0x33006D8A),
		secondary = if (darkTheme) Color(0x22D5DEE8) else Color(0x2294A1AE),
	)
}

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
