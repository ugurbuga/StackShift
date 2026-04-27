package com.ugurbuga.stackshift.game.logic

import com.ugurbuga.stackshift.game.model.SpecialBlockType
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreCalculatorTest {

    @Test
    fun calculates_only_block_points_when_no_clear_or_bonus_exists() {
        val calculator = ScoreCalculator()

        val breakdown = calculator.calculate(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 4,
                linesCleared = 0,
                currentStreak = 0,
            ),
        )

        assertEquals(40, breakdown.rawBlockScore)
        assertEquals(0, breakdown.clearBonusScore)
        assertEquals(40, breakdown.blockScore)
        assertEquals(0, breakdown.lineScore)
        assertEquals(40, breakdown.subtotalBeforeMultipliers)
        assertEquals(1f, breakdown.totalMultiplier)
        assertEquals(40, breakdown.totalScore)
    }

    @Test
    fun applies_requested_line_combo_streak_and_bonus_rules() {
        val calculator = ScoreCalculator()

        val breakdown = calculator.calculate(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 4,
                linesCleared = 2,
                currentStreak = 3,
                specialBlocksTriggered = listOf(SpecialBlockType.RowClearer),
                areaTilesCleared = 0,
                isBoardCleared = false,
                isPerfectPlacement = true,
                isHardPlacement = true,
                moveDurationMillis = 1_500L,
                boardFillRatio = 0.85f,
                chainReactionCount = 1,
            ),
        )

        assertEquals(40, breakdown.rawBlockScore)
        assertEquals(20, breakdown.clearBonusScore)
        assertEquals(60, breakdown.blockScore)
        assertEquals(250, breakdown.lineScore)
        assertEquals(150, breakdown.specialBlockBonus)
        assertEquals(50, breakdown.perfectPlacementBonus)
        assertEquals(30, breakdown.hardPlacementBonus)
        assertEquals(100, breakdown.chainReactionBonus)
        assertEquals(640, breakdown.subtotalBeforeMultipliers)
        assertEquals(1.2f, breakdown.comboMultiplier)
        assertEquals(1.25f, breakdown.streakMultiplier)
        assertEquals(1.1f, breakdown.speedMultiplier)
        assertEquals(1.3f, breakdown.riskMultiplier)
        assertEquals(1372, breakdown.totalScore)
    }

    @Test
    fun applies_highest_threshold_for_four_or_more_lines_and_eight_or_more_streak() {
        val calculator = ScoreCalculator()

        val breakdown = calculator.calculate(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 5,
                linesCleared = 6,
                currentStreak = 9,
            ),
        )

        assertEquals(75, breakdown.blockScore)
        assertEquals(700, breakdown.lineScore)
        assertEquals(2f, breakdown.comboMultiplier)
        assertEquals(2f, breakdown.streakMultiplier)
        assertEquals(775 * 4, breakdown.totalScore)
    }

    @Test
    fun supports_custom_balance_configuration() {
        val calculator = ScoreCalculator(
            ScoreConfig(
                block = BlockScoreConfig(
                    pointsPerTile = 3,
                    clearBonusMultiplier = 2f,
                ),
                lineClear = LineClearScoreConfig(
                    pointsByLineCount = mapOf(1 to 20, 2 to 40),
                    comboMultipliersByLineCount = mapOf(2 to 3f),
                ),
                streak = StreakScoreConfig(
                    multipliersByStreakCount = mapOf(2 to 2f),
                ),
                special = SpecialScoreConfig(
                    lineClearBlockBonus = 50,
                    areaClearPointsPerTile = 4,
                ),
                bonuses = BonusScoreConfig(
                    perfectClearBonus = 300,
                    perfectPlacementBonus = 11,
                    hardPlacementBonus = 7,
                    chainReactionBonusPerChain = 25,
                ),
                situationalMultipliers = SituationalMultiplierConfig(
                    fastMoveThresholdMillis = 3_000L,
                    fastMoveMultiplier = 1.5f,
                    riskThreshold = 0.5f,
                    riskMultiplier = 2f,
                ),
            ),
        )

        val total = calculator.calculateScore(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 3,
                linesCleared = 2,
                currentStreak = 2,
                specialBlocksTriggered = listOf(SpecialBlockType.ColumnClearer),
                areaTilesCleared = 2,
                isBoardCleared = true,
                isPerfectPlacement = true,
                isHardPlacement = true,
                moveDurationMillis = 2_500L,
                boardFillRatio = 0.75f,
                chainReactionCount = 2,
            ),
        )

        assertEquals(8_712, total)
    }
}


