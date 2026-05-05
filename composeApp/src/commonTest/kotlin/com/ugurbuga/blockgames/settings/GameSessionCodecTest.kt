package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameSessionCodecTest {
    private val logic = GameLogic.create(random = Random(21))

    @Test
    fun encodeDecode_preservesTimeAttackSessionDetails() {
        val challenge = DailyChallenge(
            year = 2026,
            month = 4,
            day = 28,
            style = GameplayStyle.BlockWise,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.ClearRows, target = 4, current = 2),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 1200, current = 450),
            ),
        )
        val state = logic.newGame(
            config = GameConfig(columns = 6, rows = 8),
            challenge = challenge,
            mode = GameMode.TimeAttack,
        ).copy(
            remainingTimeMillis = 42_000L,
            recentlyClearedColumns = setOf(1, 4),
            rewardedReviveUsed = true,
            activeChallenge = challenge,
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameMode.TimeAttack, decoded.gameMode)
        assertEquals(42_000L, decoded.remainingTimeMillis)
        assertEquals(setOf(1, 4), decoded.recentlyClearedColumns)
        assertTrue(decoded.rewardedReviveUsed)
        assertNotNull(decoded.activeChallenge)
        assertEquals(challenge.year, decoded.activeChallenge.year)
        assertEquals(challenge.month, decoded.activeChallenge.month)
        assertEquals(challenge.day, decoded.activeChallenge.day)
        assertEquals(challenge.tasks, decoded.activeChallenge.tasks)
    }

    @Test
    fun sessionSlotFor_routesClassicTimeAttackAndChallengeSeparately() {
        val classicState = logic.newGame()
        val timeAttackState = logic.newGame(mode = GameMode.TimeAttack)
        val challenge = DailyChallenge(
            year = 2026,
            month = 4,
            day = 28,
            style = GameplayStyle.BlockWise,
            tasks = listOf(ChallengeTask(type = ChallengeTaskType.PlacePieces, target = 6)),
        )
        val challengeState = logic.newGame(challenge = challenge)

        assertEquals(GameSessionSlot.Classic, classicState.sessionSlot())
        assertEquals(GameSessionSlot.TimeAttack, timeAttackState.sessionSlot())
        assertEquals(GameSessionSlot.DailyChallenge("2026-04-28"), challengeState.sessionSlot())
    }
}

