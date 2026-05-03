package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameplayStyle
import kotlin.random.Random

object ChallengeGenerator {
    fun generate(
        year: Int,
        month: Int,
        day: Int,
        gameplayStyle: GameplayStyle,
    ): DailyChallenge {
        val seed = (year * 10000 + month * 100 + day).toLong()
        val random = Random(seed)

        val taskCount = random.nextInt(2, 4)
        val tasks = mutableListOf<ChallengeTask>()

        val types = ChallengeTaskType.forStyle(gameplayStyle).toMutableList()
        repeat(taskCount) {
            val type = types.removeAt(random.nextInt(types.size))
            val target = when (type) {
                ChallengeTaskType.ClearBlocks -> (random.nextInt(10, 25) * 10)
                ChallengeTaskType.ReachScore -> when (gameplayStyle) {
                    GameplayStyle.StackShift -> (random.nextInt(5, 20) * 1000)
                    GameplayStyle.BlockWise -> random.nextInt(2, 8) * 1000
                    GameplayStyle.MergeShift -> (random.nextInt(5, 15) * 1000)
                }
                ChallengeTaskType.TriggerSpecial -> random.nextInt(2, 6)
                ChallengeTaskType.PerfectPlacement -> random.nextInt(10, 20)
                ChallengeTaskType.ChainReaction -> random.nextInt(1, 3)
                ChallengeTaskType.ClearRows -> random.nextInt(3, 9)
                ChallengeTaskType.ClearColumns -> random.nextInt(3, 9)
                ChallengeTaskType.PlacePieces -> random.nextInt(12, 28)
                ChallengeTaskType.ClearBothDirections -> random.nextInt(1, 4)
            }
            tasks.add(ChallengeTask(type, target))
        }

        return DailyChallenge(year, month, day, tasks)
    }
}
