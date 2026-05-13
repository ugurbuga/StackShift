package com.ugurbuga.blockgames.ui.game.dailychallenge

import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.challenge_info_clear_both_directions
import blockgames.composeapp.generated.resources.challenge_info_clear_columns_title
import blockgames.composeapp.generated.resources.challenge_info_clear_rows_title
import blockgames.composeapp.generated.resources.challenge_info_place_pieces
import blockgames.composeapp.generated.resources.challenge_info_reach_score
import blockgames.composeapp.generated.resources.challenge_info_trigger_special_title
import blockgames.composeapp.generated.resources.month_december
import blockgames.composeapp.generated.resources.month_january
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyChallengeScreenCommonTest {

    @Test
    fun challengeInfoTitleRes_returnsExpectedResourcesForRepresentativeTasks() {
        assertEquals(Res.string.challenge_info_clear_rows_title, challengeInfoTitleRes(ChallengeTaskType.ClearRows))
        assertEquals(Res.string.challenge_info_clear_columns_title, challengeInfoTitleRes(ChallengeTaskType.ClearColumns))
        assertEquals(Res.string.challenge_info_trigger_special_title, challengeInfoTitleRes(ChallengeTaskType.TriggerSpecial))
    }

    @Test
    fun challengeInfoDescriptionRes_returnsExpectedResourcesForRepresentativeTasks() {
        assertEquals(Res.string.challenge_info_reach_score, challengeInfoDescriptionRes(ChallengeTaskType.ReachScore))
        assertEquals(Res.string.challenge_info_place_pieces, challengeInfoDescriptionRes(ChallengeTaskType.PlacePieces))
        assertEquals(Res.string.challenge_info_clear_both_directions, challengeInfoDescriptionRes(ChallengeTaskType.ClearBothDirections))
    }

    @Test
    fun monthNameRes_mapsCalendarBoundsAndRejectsOutOfRangeValues() {
        assertEquals(Res.string.month_january, monthNameRes(1))
        assertEquals(Res.string.month_december, monthNameRes(12))
        assertNull(monthNameRes(0))
        assertNull(monthNameRes(13))
    }

    @Test
    fun getPreviousMonths_wrapsAcrossYearBoundaryInChronologicalOrder() {
        assertEquals(
            listOf(
                YearMonth(year = 2025, month = 11),
                YearMonth(year = 2025, month = 12),
                YearMonth(year = 2026, month = 1),
            ),
            getPreviousMonths(year = 2026, month = 1, count = 3),
        )
    }

    @Test
    fun getDaysInMonth_appliesLeapYearRules() {
        assertEquals(29, getDaysInMonth(year = 2024, month = 2))
        assertEquals(28, getDaysInMonth(year = 2100, month = 2))
        assertEquals(29, getDaysInMonth(year = 2000, month = 2))
        assertEquals(30, getDaysInMonth(year = 2026, month = 4))
    }
}

