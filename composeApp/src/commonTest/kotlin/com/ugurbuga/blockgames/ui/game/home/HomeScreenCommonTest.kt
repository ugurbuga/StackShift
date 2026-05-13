package com.ugurbuga.blockgames.ui.game.home

import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.game.model.CellTone
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScreenCommonTest {

    @Test
    fun normalizedPhase_wrapsNegativeAndOverflowValuesIntoUnitRange() {
        assertEquals(0.75f, normalizedPhase(-0.25f), absoluteTolerance = 0.0001f)
        assertEquals(0.25f, normalizedPhase(1.25f), absoluteTolerance = 0.0001f)
        assertEquals(0f, normalizedPhase(2f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun segmentProgress_clampsOutsideRangeAndInterpolatesWithinRange() {
        assertEquals(0f, segmentProgress(phase = 0.10f, start = 0.20f, end = 0.40f), absoluteTolerance = 0.0001f)
        assertEquals(0.5f, segmentProgress(phase = 0.30f, start = 0.20f, end = 0.40f), absoluteTolerance = 0.0001f)
        assertEquals(1f, segmentProgress(phase = 0.50f, start = 0.20f, end = 0.40f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun clearFlashAlpha_isSymmetricAroundMidpoint() {
        assertEquals(0f, clearFlashAlpha(0f), absoluteTolerance = 0.0001f)
        assertEquals(1f, clearFlashAlpha(0.5f), absoluteTolerance = 0.0001f)
        assertEquals(clearFlashAlpha(0.25f), clearFlashAlpha(0.75f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun titleRowAlphaFunctions_followHideAndRelaunchWindows() {
        assertEquals(1f, titleTopRowAlpha(phase = 0.20f, upperExplode = 0.3f, stackLaunch = 0.8f), absoluteTolerance = 0.0001f)
        assertEquals(0.7f, titleTopRowAlpha(phase = 0.60f, upperExplode = 0.3f, stackLaunch = 0.8f), absoluteTolerance = 0.0001f)
        assertEquals(0f, titleTopRowAlpha(phase = 0.70f, upperExplode = 0.3f, stackLaunch = 0.8f), absoluteTolerance = 0.0001f)
        assertEquals(0.8f, titleTopRowAlpha(phase = 0.80f, upperExplode = 0.3f, stackLaunch = 0.8f), absoluteTolerance = 0.0001f)

        assertEquals(1f, titleBottomRowAlpha(phase = 0.10f, lowerExplode = 0.2f, shiftLaunch = 0.6f), absoluteTolerance = 0.0001f)
        assertEquals(0.8f, titleBottomRowAlpha(phase = 0.30f, lowerExplode = 0.2f, shiftLaunch = 0.6f), absoluteTolerance = 0.0001f)
        assertEquals(0f, titleBottomRowAlpha(phase = 0.70f, lowerExplode = 0.2f, shiftLaunch = 0.6f), absoluteTolerance = 0.0001f)
        assertEquals(0.6f, titleBottomRowAlpha(phase = 0.90f, lowerExplode = 0.2f, shiftLaunch = 0.6f), absoluteTolerance = 0.0001f)
    }

    @Test
    fun homeTitleOccupiedCenterOffset_averagesOccupiedIndices() {
        val cells = listOf(
            null,
            HomeTitleCell(letter = "S", tone = CellTone.Cyan),
            null,
            HomeTitleCell(letter = "T", tone = CellTone.Gold),
            null,
            null,
        )

        assertEquals(20.dp, homeTitleOccupiedCenterOffset(cells = cells, cellSize = 10.dp))
        assertEquals(0.dp, homeTitleOccupiedCenterOffset(cells = List(6) { null }, cellSize = 10.dp))
    }
}

