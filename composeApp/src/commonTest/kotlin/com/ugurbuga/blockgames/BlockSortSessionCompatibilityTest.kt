package com.ugurbuga.blockgames

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.settings.GameSessionSlot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockSortSessionCompatibilityTest {

    @Test
    fun isUsableSavedSession_acceptsBlockSortRoundsWithDynamicBoardSize() {
        var board = BoardMatrix.empty(columns = 7, rows = 5)
        board = board.fill(points = listOf(GridPoint(0, 4)), tone = CellTone.Gold, value = 1)
        board = board.fill(points = listOf(GridPoint(1, 4)), tone = CellTone.Cyan, value = 2)

        val state = GameState(
            config = GameConfig(columns = 7, rows = 5, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            gameMode = GameMode.Classic,
            gameplayStyle = GameplayStyle.BlockSort,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 120,
            linesCleared = 3,
            level = 3,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            status = GameStatus.Running,
        )

        assertTrue(isUsableSavedSession(state, GameSessionSlot.Classic(GameplayStyle.BlockSort)))
    }

    @Test
    fun isUsableSavedSession_stillRejectsNonDefaultBoardSizesForStaticModes() {
        val state = GameState(
            config = GameConfig(columns = 11, rows = 11),
            gameMode = GameMode.Classic,
            gameplayStyle = GameplayStyle.StackShift,
            board = BoardMatrix.empty(columns = 11, rows = 11),
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 18,
            status = GameStatus.Running,
        )

        assertFalse(isUsableSavedSession(state, GameSessionSlot.Classic(GameplayStyle.StackShift)))
    }
}

