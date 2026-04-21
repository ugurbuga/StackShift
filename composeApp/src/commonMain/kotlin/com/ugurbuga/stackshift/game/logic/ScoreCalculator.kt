package com.ugurbuga.stackshift.game.logic

import com.ugurbuga.stackshift.game.model.SpecialBlockType

data class BlockScoreConfig(
    val pointsPerTile: Int = 10,
    val clearBonusMultiplier: Float = 1.5f,
)

data class LineClearScoreConfig(
    val pointsByLineCount: Map<Int, Int> = mapOf(
        1 to 100,
        2 to 250,
        3 to 450,
        4 to 700,
    ),
    val comboMultipliersByLineCount: Map<Int, Float> = mapOf(
        2 to 1.2f,
        3 to 1.5f,
        4 to 2f,
    ),
)

data class StreakScoreConfig(
    val multipliersByStreakCount: Map<Int, Float> = mapOf(
        2 to 1.1f,
        3 to 1.25f,
        5 to 1.5f,
        8 to 2f,
    ),
)

data class SpecialScoreConfig(
    val lineClearBlockBonus: Int = 150,
    val areaClearPointsPerTile: Int = 20,
)

data class BonusScoreConfig(
    val perfectClearBonus: Int = 1500,
    val perfectPlacementBonus: Int = 50,
    val hardPlacementBonus: Int = 30,
    val chainReactionBonusPerChain: Int = 100,
)

data class SituationalMultiplierConfig(
    val fastMoveThresholdMillis: Long = 2_000L,
    val fastMoveMultiplier: Float = 1.1f,
    val riskThreshold: Float = 0.8f,
    val riskMultiplier: Float = 1.3f,
)

data class ScoreConfig(
    val block: BlockScoreConfig = BlockScoreConfig(),
    val lineClear: LineClearScoreConfig = LineClearScoreConfig(),
    val streak: StreakScoreConfig = StreakScoreConfig(),
    val special: SpecialScoreConfig = SpecialScoreConfig(),
    val bonuses: BonusScoreConfig = BonusScoreConfig(),
    val situationalMultipliers: SituationalMultiplierConfig = SituationalMultiplierConfig(),
)

/**
 * Esnek ve balans ayarına uygun skor hesaplayıcısı.
 *
 * Örnek kullanım:
 * ```kotlin
 * val calculator = ScoreCalculator(
 *     ScoreConfig(
 *         block = BlockScoreConfig(pointsPerTile = 10, clearBonusMultiplier = 1.5f),
 *         lineClear = LineClearScoreConfig(
 *             pointsByLineCount = mapOf(1 to 100, 2 to 250, 3 to 450, 4 to 700),
 *             comboMultipliersByLineCount = mapOf(2 to 1.2f, 3 to 1.5f, 4 to 2f),
 *         ),
 *     ),
 * )
 *
 * val breakdown = calculator.calculate(
 *     ScoreCalculator.ScoreParams(
 *         tilesPlaced = 4,
 *         linesCleared = 2,
 *         currentStreak = 3,
 *         specialBlocksTriggered = listOf(SpecialBlockType.RowClearer),
 *         areaTilesCleared = 0,
 *         isBoardCleared = false,
 *         isPerfectPlacement = true,
 *         isHardPlacement = false,
 *         moveDurationMillis = 1_500L,
 *         boardFillRatio = 0.84f,
 *         chainReactionCount = 1,
 *     ),
 * )
 *
 * val totalScore = breakdown.totalScore
 * ```
 */
class ScoreCalculator(
    private val config: ScoreConfig = ScoreConfig(),
) {

    data class ScoreParams(
        val tilesPlaced: Int,
        val linesCleared: Int,
        val currentStreak: Int,
        val specialBlocksTriggered: List<SpecialBlockType> = emptyList(),
        val areaTilesCleared: Int = 0,
        val isBoardCleared: Boolean = false,
        val isPerfectPlacement: Boolean = false,
        val isHardPlacement: Boolean = false,
        val moveDurationMillis: Long? = null,
        val boardFillRatio: Float = 0f,
        val chainReactionCount: Int = 0,
    )

    data class ScoreBreakdown(
        val rawBlockScore: Int,
        val clearBonusScore: Int,
        val blockScore: Int,
        val lineScore: Int,
        val specialBlockBonus: Int,
        val areaClearBonus: Int,
        val perfectClearBonus: Int,
        val perfectPlacementBonus: Int,
        val hardPlacementBonus: Int,
        val chainReactionBonus: Int,
        val subtotalBeforeMultipliers: Int,
        val comboMultiplier: Float,
        val streakMultiplier: Float,
        val speedMultiplier: Float,
        val riskMultiplier: Float,
        val totalMultiplier: Float,
        val totalScore: Int,
    )

    fun calculate(params: ScoreParams): ScoreBreakdown {
        require(params.tilesPlaced >= 0) { "tilesPlaced negatif olamaz" }
        require(params.linesCleared >= 0) { "linesCleared negatif olamaz" }
        require(params.currentStreak >= 0) { "currentStreak negatif olamaz" }
        require(params.areaTilesCleared >= 0) { "areaTilesCleared negatif olamaz" }
        require(params.chainReactionCount >= 0) { "chainReactionCount negatif olamaz" }

        val rawBlockScore = params.tilesPlaced * config.block.pointsPerTile
        val clearTriggered = params.linesCleared > 0 ||
            params.specialBlocksTriggered.isNotEmpty() ||
            params.areaTilesCleared > 0 ||
            params.isBoardCleared
        val boostedBlockScore = if (clearTriggered) {
            (rawBlockScore * config.block.clearBonusMultiplier).toInt()
        } else {
            rawBlockScore
        }
        val clearBonusScore = boostedBlockScore - rawBlockScore

        val lineScore = thresholdValue(
            input = params.linesCleared,
            thresholds = config.lineClear.pointsByLineCount,
            defaultValue = 0,
        )

        val specialBlockBonus = params.specialBlocksTriggered.count {
            it == SpecialBlockType.RowClearer || it == SpecialBlockType.ColumnClearer
        } * config.special.lineClearBlockBonus
        val areaClearBonus = params.areaTilesCleared * config.special.areaClearPointsPerTile
        val perfectClearBonus = if (params.isBoardCleared) config.bonuses.perfectClearBonus else 0
        val perfectPlacementBonus = if (params.isPerfectPlacement) config.bonuses.perfectPlacementBonus else 0
        val hardPlacementBonus = if (params.isHardPlacement) config.bonuses.hardPlacementBonus else 0
        val chainReactionBonus = params.chainReactionCount * config.bonuses.chainReactionBonusPerChain

        val subtotalBeforeMultipliers = boostedBlockScore +
            lineScore +
            specialBlockBonus +
            areaClearBonus +
            perfectClearBonus +
            perfectPlacementBonus +
            hardPlacementBonus +
            chainReactionBonus

        val comboMultiplier = thresholdValue(
            input = params.linesCleared,
            thresholds = config.lineClear.comboMultipliersByLineCount,
            defaultValue = 1f,
        )
        val streakMultiplier = thresholdValue(
            input = params.currentStreak,
            thresholds = config.streak.multipliersByStreakCount,
            defaultValue = 1f,
        )
        val speedMultiplier = if (
            params.moveDurationMillis != null &&
            params.moveDurationMillis in 1 until config.situationalMultipliers.fastMoveThresholdMillis
        ) {
            config.situationalMultipliers.fastMoveMultiplier
        } else {
            1f
        }
        val riskMultiplier = if (params.boardFillRatio > config.situationalMultipliers.riskThreshold) {
            config.situationalMultipliers.riskMultiplier
        } else {
            1f
        }
        val totalMultiplier = comboMultiplier * streakMultiplier * speedMultiplier * riskMultiplier
        val totalScore = (subtotalBeforeMultipliers * totalMultiplier).toInt()

        return ScoreBreakdown(
            rawBlockScore = rawBlockScore,
            clearBonusScore = clearBonusScore,
            blockScore = boostedBlockScore,
            lineScore = lineScore,
            specialBlockBonus = specialBlockBonus,
            areaClearBonus = areaClearBonus,
            perfectClearBonus = perfectClearBonus,
            perfectPlacementBonus = perfectPlacementBonus,
            hardPlacementBonus = hardPlacementBonus,
            chainReactionBonus = chainReactionBonus,
            subtotalBeforeMultipliers = subtotalBeforeMultipliers,
            comboMultiplier = comboMultiplier,
            streakMultiplier = streakMultiplier,
            speedMultiplier = speedMultiplier,
            riskMultiplier = riskMultiplier,
            totalMultiplier = totalMultiplier,
            totalScore = totalScore,
        )
    }

    fun calculateScore(params: ScoreParams): Int = calculate(params).totalScore

    private fun thresholdValue(
        input: Int,
        thresholds: Map<Int, Int>,
        defaultValue: Int,
    ): Int {
        if (input <= 0) return defaultValue
        return thresholds.entries
            .filter { it.key <= input }
            .maxByOrNull { it.key }
            ?.value
            ?: defaultValue
    }

    private fun thresholdValue(
        input: Int,
        thresholds: Map<Int, Float>,
        defaultValue: Float,
    ): Float {
        if (input <= 0) return defaultValue
        return thresholds.entries
            .filter { it.key <= input }
            .maxByOrNull { it.key }
            ?.value
            ?: defaultValue
    }
}
