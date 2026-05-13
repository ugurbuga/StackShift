package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.logic.BlockSortGameLogic
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.model.GridPoint
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlockSortOnboardingStateFactoryTest {

    private val logic = BlockSortGameLogic()

    @Test
    fun everyOnboardingScene_hasValidGuidedMove() {
        BlockSortOnboardingStateFactory.stages.forEach { stage ->
            val scene = BlockSortOnboardingStateFactory.scene(stage)
            val targetColumn = scene.acceptedTargetColumns.first()

            val preview = logic.previewPlacement(
                state = scene.gameState,
                pieceId = scene.guideSourceColumn.toLong(),
                origin = GridPoint(targetColumn, 0),
            )
            assertNotNull(preview, "stage $stage should expose a valid guided preview")

            val result = logic.placePiece(
                state = scene.gameState,
                pieceId = scene.guideSourceColumn.toLong(),
                origin = GridPoint(targetColumn, 0),
            )
            assertTrue(GameEvent.PlacementAccepted in result.events, "stage $stage should accept the guided move")
        }
    }
}

