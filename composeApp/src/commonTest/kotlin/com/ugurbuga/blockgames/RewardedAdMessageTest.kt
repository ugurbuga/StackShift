package com.ugurbuga.blockgames

import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.game_message_ad_reward_blockwise
import blockgames.composeapp.generated.resources.game_message_ad_reward_boomblocks
import blockgames.composeapp.generated.resources.game_message_ad_reward_mergeshift
import blockgames.composeapp.generated.resources.reward_piece_column_message
import blockgames.composeapp.generated.resources.reward_piece_row_message
import com.ugurbuga.blockgames.game.logic.BlockWiseGameLogic
import com.ugurbuga.blockgames.game.logic.BoomBlocksGameLogic
import com.ugurbuga.blockgames.game.logic.MergeShiftGameLogic
import com.ugurbuga.blockgames.game.logic.StackShiftGameLogic
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardedAdMessageTest {

    @Test
    fun rewardedDockFeedbackSpec_returnsStyleSpecificMessagesForNonStackShiftGames() {
        assertEquals(
            Res.string.game_message_ad_reward_blockwise,
            rewardedDockFeedbackSpec(GameplayStyle.BlockWise, SpecialBlockType.None).messageRes,
        )
        assertEquals(
            Res.string.game_message_ad_reward_mergeshift,
            rewardedDockFeedbackSpec(GameplayStyle.MergeShift, SpecialBlockType.None).messageRes,
        )
        assertEquals(
            Res.string.game_message_ad_reward_boomblocks,
            rewardedDockFeedbackSpec(GameplayStyle.BoomBlocks, SpecialBlockType.None).messageRes,
        )
    }

    @Test
    fun rewardedDockFeedbackSpec_keepsStackShiftSpecialMessages() {
        assertEquals(
            Res.string.reward_piece_row_message,
            rewardedDockFeedbackSpec(
                gameplayStyle = GameplayStyle.StackShift,
                specialType = SpecialBlockType.RowClearer,
            ).messageRes,
        )
        assertEquals(
            Res.string.reward_piece_column_message,
            rewardedDockFeedbackSpec(
                gameplayStyle = GameplayStyle.StackShift,
                specialType = SpecialBlockType.ColumnClearer,
            ).messageRes,
        )
    }

    @Test
    fun adRewardGameTextKeys_areAppendedAfterLegacyEntries_forSessionCompatibility() {
        val legacyBoundary = GameTextKey.PiecePropertiesNone.ordinal

        assertTrue(GameTextKey.GameMessageAdRewardBlockWise.ordinal > legacyBoundary)
        assertTrue(GameTextKey.GameMessageAdRewardMergeShift.ordinal > legacyBoundary)
        assertTrue(GameTextKey.GameMessageAdRewardBoomBlocks.ordinal > legacyBoundary)
        assertTrue(GameTextKey.GameMessageAdRewardStackShift.ordinal > legacyBoundary)
        assertTrue(GameTextKey.FeedbackAdRewardBlockWise.ordinal > legacyBoundary)
        assertTrue(GameTextKey.FeedbackAdRewardMergeShift.ordinal > legacyBoundary)
        assertTrue(GameTextKey.FeedbackAdRewardBoomBlocks.ordinal > legacyBoundary)
    }

    @Test
    fun styleSpecificGameLogics_publishTheirOwnRewardMessages() {
        val classicMode = GameMode.Classic

        val blockWiseState = BlockWiseGameLogic(random = Random(1)).newGame(
            config = GameConfig.default(GameplayStyle.BlockWise),
            challenge = null,
            mode = classicMode,
        )
        val blockWiseRewarded = BlockWiseGameLogic(random = Random(1)).replaceActivePiece(
            state = blockWiseState,
            specialType = SpecialBlockType.None,
        ).state
        assertEquals(GameTextKey.GameMessageAdRewardBlockWise, blockWiseRewarded.message.key)
        assertEquals(GameTextKey.FeedbackAdRewardBlockWise, blockWiseRewarded.floatingFeedback?.text?.key)

        val mergeShiftState = MergeShiftGameLogic(random = Random(2)).newGame(
            config = GameConfig.default(GameplayStyle.MergeShift),
            challenge = null,
            mode = classicMode,
        )
        val mergeShiftRewarded = MergeShiftGameLogic(random = Random(2)).replaceActivePiece(
            state = mergeShiftState,
            specialType = SpecialBlockType.None,
        ).state
        assertEquals(GameTextKey.GameMessageAdRewardMergeShift, mergeShiftRewarded.message.key)
        assertEquals(GameTextKey.FeedbackAdRewardMergeShift, mergeShiftRewarded.floatingFeedback?.text?.key)

        val boomBlocksState = BoomBlocksGameLogic(random = Random(3)).newGame(
            config = GameConfig.default(GameplayStyle.BoomBlocks),
            challenge = null,
            mode = classicMode,
        )
        val boomBlocksRewarded = BoomBlocksGameLogic(random = Random(3)).replaceActivePiece(
            state = boomBlocksState,
            specialType = SpecialBlockType.None,
        ).state
        assertEquals(GameTextKey.GameMessageAdRewardBoomBlocks, boomBlocksRewarded.message.key)
        assertEquals(GameTextKey.FeedbackAdRewardBoomBlocks, boomBlocksRewarded.floatingFeedback?.text?.key)
    }

    @Test
    fun stackShiftRewardedSpecial_usesStackShiftSpecificFeedback() {
        val logic = StackShiftGameLogic(random = Random(4))
        val state = logic.newGame(
            config = GameConfig.default(GameplayStyle.StackShift),
            challenge = null,
            mode = GameMode.Classic,
        )

        val rewarded = logic.replaceActivePiece(
            state = state,
            specialType = SpecialBlockType.ColumnClearer,
        ).state

        assertEquals(GameTextKey.GameMessageAdRewardStackShift, rewarded.message.key)
        assertEquals(GameTextKey.GameMessageAdRewardStackShift, rewarded.floatingFeedback?.text?.key)
    }
}



