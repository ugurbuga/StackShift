package com.ugurbuga.stackshift.platform.feedback

import androidx.compose.runtime.Stable

enum class GameSound {
    Grab,
    DropSuccess,
    DropInvalid,
    LineClear,
    Combo,
    Pause,
    Resume,
    GameOver,
    Restart,
}

enum class GameHaptic {
    Light,
    Medium,
    Heavy,
    Success,
    Warning,
}

@Stable
interface SoundEffectPlayer {
    fun play(effect: GameSound)
}

@Stable
interface GameHaptics {
    fun perform(effect: GameHaptic)
}

object NoOpSoundEffectPlayer : SoundEffectPlayer {
    override fun play(effect: GameSound) = Unit
}

object NoOpGameHaptics : GameHaptics {
    override fun perform(effect: GameHaptic) = Unit
}

