package com.ugurbuga.stackshift.game.logic

import com.ugurbuga.stackshift.game.model.ChallengeTask
import com.ugurbuga.stackshift.game.model.ChallengeTaskType
import com.ugurbuga.stackshift.game.model.DailyChallenge

object ChallengeGenerator {
    fun generate(year: Int, month: Int, day: Int): DailyChallenge {
        val seed = (year * 10000 + month * 100 + day).toLong()
        val random = kotlin.random.Random(seed)

        val taskCount = random.nextInt(2, 4)
        val tasks = mutableListOf<ChallengeTask>()

        val types = ChallengeTaskType.entries.toMutableList()
        repeat(taskCount) {
            val type = types.removeAt(random.nextInt(types.size))
            val target = when (type) {
                ChallengeTaskType.ClearBlocks -> (random.nextInt(10, 25) * 10) // 100-250 blocks
                ChallengeTaskType.ReachScore -> (random.nextInt(5, 20) * 1000) // 5k-20k score
                ChallengeTaskType.TriggerSpecial -> random.nextInt(2, 6)
                ChallengeTaskType.PerfectPlacement -> random.nextInt(2, 5)
                ChallengeTaskType.ChainReaction -> random.nextInt(1, 3)
            }
            tasks.add(ChallengeTask(type, target))
        }

        return DailyChallenge(year, month, day, tasks)
    }
}
