package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FirstRunGameOnboardingStateFactoryTest {
    private val logic = GameLogic.create()
    private val onboardingStages = FirstRunGameOnboardingStateFactory.stages

    @Test
    fun everyStageUsesTenBySixBoardConfig() {
        onboardingStages.forEach { stage ->
            val config = FirstRunGameOnboardingStateFactory.scene(stage).gameState.config
            assertEquals(10, config.columns, "Stage $stage should use 10 columns during interactive onboarding")
            assertEquals(6, config.rows, "Stage $stage should use 6 rows during interactive onboarding")
        }
    }

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
            val placement = guidedDrop(scene)
            assertTrue(
                GameEvent.PlacementAccepted in placement.events,
                "Stage $stage should accept the guided placement immediately",
            )
            assertNotNull(placement.state.activePiece, "Stage $stage should continue with the next piece after completion")
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
        val placement = guidedDrop(scene)

        assertTrue(GameEvent.LineClear in placement.events, "Line clear stage should trigger a standard line clear")
        assertTrue(GameEvent.SpecialTriggered !in placement.events, "Line clear stage should not depend on a special block")
        assertTrue(
            placement.state.recentlyClearedRows.isNotEmpty(),
            "Normal line-clear stage should record the exact row completed by the guided drop",
        )
        assertTrue(placement.state.lastResolvedLines > 0, "Normal line-clear stage should resolve at least one full row")
    }

    @Test
    fun rowClearerGuidedDropStillDemonstratesALineClear() {
        val scene = FirstRunGameOnboardingStateFactory.scene(FirstRunOnboardingStage.RowClearer)
        val placement = guidedDrop(scene)

        assertTrue(GameEvent.SpecialTriggered in placement.events, "Row clearer stage should demonstrate a triggered special piece")
        assertTrue(
            placement.state.recentlyClearedRows.isNotEmpty() || placement.state.recentlyClearedColumns.isNotEmpty(),
            "Row clearer stage should leave the targeted horizontal rows empty after the guided drop",
        )
    }

    @Test
    fun columnClearerStageTriggersAColumnClear() {
        val scene = FirstRunGameOnboardingStateFactory.scene(FirstRunOnboardingStage.ColumnClearer)
        val placement = guidedDrop(scene)

        assertTrue(GameEvent.SpecialTriggered in placement.events, "Column clear stage should demonstrate a triggered special piece")
        assertTrue(
            placement.state.recentlyClearedColumns.isNotEmpty(),
            "Column clear stage should record the exact column completed by the guided drop",
        )
    }

    @Test
    fun heavyStageReshapesTheBoardAfterTriggeringTheHeavySpecial() {
        val scene = FirstRunGameOnboardingStateFactory.scene(FirstRunOnboardingStage.Heavy)
        val placement = guidedDrop(scene)

        assertTrue(GameEvent.SpecialTriggered in placement.events, "Heavy stage should demonstrate a triggered heavy piece")
        assertTrue(
            placement.state.board != scene.gameState.board,
            "Heavy stage should reshape the board after the guided drop",
        )
    }

    @Test
    fun cleanGameStateStartsWithEmptyBoardAndZeroProgress() {
        val state = FirstRunGameOnboardingStateFactory.cleanGameState()

        assertTrue((0 until state.config.columns).all(state.board::isColumnEmpty), "Clean onboarding handoff should start with an empty board")
        assertTrue(state.score == 0, "Clean onboarding handoff should reset the score")
        assertTrue(state.linesCleared == 0, "Clean onboarding handoff should reset cleared lines")
    }

    private fun guidedDrop(scene: FirstRunOnboardingScene) =
        logic.placePiece(scene.gameState, scene.guideColumn ?: scene.acceptedColumns.first()).let { armedPlacement ->
            assertTrue(
                GameEvent.SoftLockStarted in armedPlacement.events || GameEvent.SoftLockAdjusted in armedPlacement.events,
                "Stage ${scene.stage} should start a soft-lock preview before commit",
            )
            logic.commitSoftLock(armedPlacement.state)
        }
}

