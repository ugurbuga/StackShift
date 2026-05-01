package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.platform.isDebugBuild
import kotlin.test.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingsThemeAccessTest {

    @Test
    fun defaultThemeMode_usesSystemSetting() {
        assertEquals(AppThemeMode.System, AppSettings().themeMode)
    }

    @Test
    fun themeModes_areAlwaysAvailableToEveryone() {
        val settings = AppSettings(unlockedThemeModes = emptySet())

        assertEquals(
            AppThemeMode.entries.toSet(),
            settings.availableThemeModes(),
        )
        assertTrue(AppThemeMode.entries.all(settings::isThemeModeUnlocked))
    }

    @Test
    fun resolvedThemePaletteUnlocks_usesClassicOnlyOutsideDebug_andAllPalettesInDebug() {
        assertEquals(
            setOf(AppColorPalette.Classic, AppColorPalette.Sunset),
            resolvedThemePaletteUnlocks(
                debugBuild = false,
                unlockedThemePalettes = setOf(AppColorPalette.Sunset),
            ),
        )
        assertEquals(
            AppColorPalette.entries.toSet(),
            resolvedThemePaletteUnlocks(
                debugBuild = true,
                unlockedThemePalettes = emptySet(),
            ),
        )
    }

    @Test
    fun sanitized_matchesCurrentBuildPaletteAccessPolicy() {
        val sanitized = AppSettings(
            themeColorPalette = AppColorPalette.Sunset,
            unlockedThemePalettes = emptySet(),
        ).sanitized()

        if (isDebugBuild()) {
            assertEquals(AppColorPalette.Sunset, sanitized.themeColorPalette)
            assertEquals(AppColorPalette.entries.toSet(), sanitized.unlockedThemePalettes)
        } else {
            assertEquals(AppColorPalette.Classic, sanitized.themeColorPalette)
            assertEquals(setOf(AppColorPalette.Classic), sanitized.unlockedThemePalettes)
        }
    }

    @Test
    fun themeModesRemainFree_andPalettesFollowBuildPolicy() {
        val initial = AppSettings(tokenBalance = 60, unlockedThemePalettes = setOf(AppColorPalette.Classic))

        val themeUpdated = initial.unlockThemeMode(AppThemeMode.Dark)
        val paletteUpdated = initial.unlockThemePalette(AppColorPalette.Sunset)

        assertEquals(AppThemeMode.Dark, themeUpdated.themeMode)
        assertEquals(60, themeUpdated.tokenBalance)

        val unlockedPalette = assertNotNull(paletteUpdated)
        assertEquals(AppColorPalette.Sunset, unlockedPalette.themeColorPalette)

        if (isDebugBuild()) {
            assertEquals(60, unlockedPalette.tokenBalance)
            assertEquals(AppColorPalette.entries.toSet(), unlockedPalette.unlockedThemePalettes)
        } else {
            assertEquals(12, unlockedPalette.tokenBalance)
            assertEquals(
                setOf(AppColorPalette.Classic, AppColorPalette.Sunset),
                unlockedPalette.unlockedThemePalettes,
            )
        }
    }
}


