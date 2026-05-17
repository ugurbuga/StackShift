package com.ugurbuga.blockgames

import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class AppStartupCommonTest {

    @Test
    fun resolveStartupRoute_opensSelectionWhenNoGameWasChosenYet() {
        assertEquals(
            AppRoute.Selection,
            resolveStartupRoute(loadedSettings = AppSettings()),
        )
    }

    @Test
    fun resolveStartupRoute_opensHomeWhenLastSelectedGameExists() {
        assertEquals(
            AppRoute.Home,
            resolveStartupRoute(
                loadedSettings = AppSettings(selectedGameplayStyle = GameplayStyle.BlockSort),
            ),
        )
    }

    @Test
    fun resolveStartupRoute_opensHomeWhenStartupOverrideIsProvided() {
        assertEquals(
            AppRoute.Home,
            resolveStartupRoute(
                loadedSettings = AppSettings(),
                startupGameplayStyleOverride = GameplayStyle.MergeShift,
            ),
        )
    }
}

