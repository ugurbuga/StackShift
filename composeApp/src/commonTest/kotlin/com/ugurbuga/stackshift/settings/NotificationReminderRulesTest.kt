package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.ChallengeProgress
import com.ugurbuga.stackshift.platform.CurrentDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationReminderRulesTest {

    @Test
    fun dailyChallengeReminder_onlyShowsWhenTodayIsIncomplete() {
        val date = CurrentDate(year = 2026, month = 4, day = 27)
        val incomplete = AppSettings()
        val complete = AppSettings(
            challengeProgress = ChallengeProgress(
                completedDays = mapOf("2026-04" to setOf(27)),
            ),
        )

        assertTrue(incomplete.shouldSendDailyChallengeReminder(date))
        assertFalse(complete.shouldSendDailyChallengeReminder(date))
    }

    @Test
    fun missYouReminder_requiresSixHoursOfInactivity() {
        val now = 50_000_000L
        val recentOpen = AppSettings(lastAppOpenedAtEpochMillis = now - (5 * 60 * 60 * 1000L))
        val staleOpen = AppSettings(lastAppOpenedAtEpochMillis = now - (7 * 60 * 60 * 1000L))

        assertFalse(recentOpen.shouldSendMissYouReminder(now))
        assertTrue(staleOpen.shouldSendMissYouReminder(now))
    }
}


