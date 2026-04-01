package com.ugurbuga.stackshift.platform.feedback

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
    Success,
    Warning,
}

interface SoundEffectPlayer {
    fun play(effect: GameSound)
}

interface GameHaptics {
    fun perform(effect: GameHaptic)
}

object NoOpSoundEffectPlayer : SoundEffectPlayer {
    override fun play(effect: GameSound) = Unit
}

object NoOpGameHaptics : GameHaptics {
    override fun perform(effect: GameHaptic) = Unit
}

