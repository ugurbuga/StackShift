package com.ugurbuga.stackshift.game.model

import androidx.compose.runtime.Immutable

@Immutable
enum class ChallengeTaskType {
    ClearBlocks,
    ReachScore,
    TriggerSpecial,
    PerfectPlacement,
    ChainReaction
}

@Immutable
data class ChallengeTask(
    val type: ChallengeTaskType,
    val target: Int,
    val current: Int = 0,
) {
    val isCompleted: Boolean get() = current >= target
}

@Immutable
data class DailyChallenge(
    val year: Int,
    val month: Int,
    val day: Int,
    val tasks: List<ChallengeTask>,
) {
    val isCompleted: Boolean get() = tasks.all { it.isCompleted }
}

@Immutable
data class ChallengeProgress(
    val completedDays: Map<String, Set<Int>> = emptyMap() // "YYYY-MM" -> Set of days
)
