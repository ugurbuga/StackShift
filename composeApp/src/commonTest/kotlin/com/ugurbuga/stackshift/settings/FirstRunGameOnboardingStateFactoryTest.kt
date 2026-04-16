package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FirstRunGameOnboardingStateFactoryTest {
    private val logic = GameLogic()
    private val onboardingStages = FirstRunGameOnboardingStateFactory.stages

    @Test
    fun everyStageExposesValidGuidance() {
        onboardingStages.forEach { stage ->
            val scene = FirstRunGameOnboardingStateFactory.scene(stage)
            val candidateColumns = when {
                scene.acceptedColumns.isNotEmpty() -> scene.acceptedColumns
                scene.guideColumn != null -> setOf(scene.guideColumn)
                else -> emptySet()
            }

            assertNotNull(scene.gameState.activePiece, "Stage $stage should have an active piece")
            assertTrue(candidateColumns.isNotEmpty(), "Stage $stage should expose at least one valid tutorial column")
            scene.guideColumn?.let { guideColumn ->
                assertTrue(guideColumn in candidateColumns, "Stage $stage should guide the player to a valid column")
            }

            candidateColumns.forEach { column ->
                assertNotNull(
                    logic.previewPlacement(scene.gameState, column),
                    "Stage $stage should allow a placement preview for tutorial column $column",
                )
            }
        }
    }

    @Test
    fun everyStageCanBeCompletedWithAGuidedDrop() {
        onboardingStages.forEach { stage ->
            val scene = FirstRunGameOnboardingStateFactory.scene(stage)
            val tutorialColumn = scene.guideColumn ?: scene.acceptedColumns.first()

            val placement = logic.placePiece(scene.gameState, tutorialColumn)
            assertTrue(
                GameEvent.SoftLockStarted in placement.events || GameEvent.SoftLockAdjusted in placement.events,
                "Stage $stage should enter soft lock after the guided drop",
            )
            assertNotNull(placement.state.softLock, "Stage $stage should produce a soft-lock state")

            val committed = logic.commitSoftLock(placement.state)
            assertTrue(
                GameEvent.PlacementAccepted in committed.events,
                "Stage $stage should accept the guided placement when committed",
            )
            assertNotNull(committed.state.activePiece, "Stage $stage should continue with the next piece after completion")
        }
    }

    @Test
    fun onboardingStagesPlaceRowClearerImmediatelyAfterDragStep() {
        assertTrue(onboardingStages.size >= 3, "Onboarding should define at least three stages")
        assertTrue(
            onboardingStages.take(3) == listOf(
                FirstRunOnboardingStage.DragAndLaunch,
                FirstRunOnboardingStage.LineClear,
                FirstRunOnboardingStage.RowClearer,
            ),
            "The guided flow should show a normal line clear before the row clearer example.",
        )
    }

    @Test
    fun normalLineClearStageTriggersARegularRowClear() {
        val scene = FirstRunGameOnboardingStateFactory.scene(FirstRunOnboardingStage.LineClear)
        val tutorialColumn = scene.guideColumn ?: scene.acceptedColumns.first()

        val placement = logic.placePiece(scene.gameState, tutorialColumn)
        val clearedRows = placement.state.softLock?.preview?.occupiedCells?.map { it.row }?.toSet()
        assertNotNull(clearedRows, "Line clear stage should produce a preview before commit")
        val committed = logic.commitSoftLock(placement.state)

        assertTrue(GameEvent.LineClear in committed.events, "Normal line-clear stage should trigger a standard line clear")
        assertTrue(GameEvent.SpecialTriggered !in committed.events, "Normal line-clear stage should not depend on a special block")
        assertTrue(
            clearedRows == committed.state.recentlyClearedRows,
            "Normal line-clear stage should record the exact row completed by the guided drop",
        )
        assertTrue(committed.state.lastResolvedLines > 0, "Normal line-clear stage should resolve at least one full row")
    }

    @Test
    fun rowClearerGuidedDropTriggersARowClear() {
        val scene = FirstRunGameOnboardingStateFactory.scene(FirstRunOnboardingStage.RowClearer)
        val tutorialColumn = scene.guideColumn ?: scene.acceptedColumns.first()

        val placement = logic.placePiece(scene.gameState, tutorialColumn)
        val clearedRows = placement.state.softLock?.preview?.occupiedCells?.map { it.row }?.toSet()
        assertNotNull(clearedRows, "Row clearer stage should produce a preview before commit")
        val committed = logic.commitSoftLock(placement.state)

        assertTrue(GameEvent.SpecialTriggered in committed.events, "Row clearer stage should trigger the row clearer special")
        assertTrue(GameEvent.LineClear !in committed.events, "Row clearer stage should demonstrate the special clear instead of a normal full-row clear")
        assertTrue(
            clearedRows.all { row -> committed.state.board.occupiedPointsInRows(setOf(row)).isEmpty() },
            "Row clearer stage should leave the targeted horizontal rows empty after the guided drop",
        )
    }

    @Test
    fun cleanGameStateStartsWithEmptyBoardAndZeroProgress() {
        val state = FirstRunGameOnboardingStateFactory.cleanGameState()

        assertTrue((0 until state.config.columns).all(state.board::isColumnEmpty), "Clean onboarding handoff should start with an empty board")
        assertTrue(state.score == 0, "Clean onboarding handoff should reset the score")
        assertTrue(state.linesCleared == 0, "Clean onboarding handoff should reset cleared lines")
    }
}

