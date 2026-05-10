package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameplayStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsOnboardingStateTest {

    @Test
    fun legacyOnboardingFlags_migrateToAllGameplayStyles() {
        val sanitized = AppSettings(
            hasSeenTutorial = true,
            hasShownInteractiveOnboarding = true,
        ).sanitized()

        assertEquals(GameplayStyle.entries.toSet(), sanitized.seenTutorialStyles)
        assertEquals(GameplayStyle.entries.toSet(), sanitized.shownInteractiveOnboardingStyles)
    }

    @Test
    fun tutorialAndInteractiveOnboarding_areTrackedPerGameplayStyle() {
        val boomBlocksOnly = AppSettings()
            .markTutorialSeen(GameplayStyle.BoomBlocks)
            .markInteractiveOnboardingShown(GameplayStyle.BoomBlocks)

        assertTrue(boomBlocksOnly.hasSeenTutorialFor(GameplayStyle.BoomBlocks))
        assertTrue(boomBlocksOnly.hasShownInteractiveOnboardingFor(GameplayStyle.BoomBlocks))
        assertFalse(boomBlocksOnly.hasSeenTutorialFor(GameplayStyle.StackShift))
        assertFalse(boomBlocksOnly.hasShownInteractiveOnboardingFor(GameplayStyle.StackShift))
    }
}

