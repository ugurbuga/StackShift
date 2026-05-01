package com.ugurbuga.blockgames.platform.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.ExperimentalResourceApi
import blockgames.composeapp.generated.resources.Res

enum class GameSound {
    Grab,
    DropSuccess,
    DropInvalid,
    LineClear,
    Combo,
    GameOver,
    Restart,
    DragLoop,
    Bgm,
    PerfectDrop,
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
    fun stop(effect: GameSound)
}

@Stable
interface GameHaptics {
    fun perform(effect: GameHaptic)
}

object NoOpSoundEffectPlayer : SoundEffectPlayer {
    override fun play(effect: GameSound) = Unit
    override fun stop(effect: GameSound) = Unit
}

object NoOpGameHaptics : GameHaptics {
    override fun perform(effect: GameHaptic) = Unit
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSoundEffectPlayer(enabled: Boolean): SoundEffectPlayer {
    var moveBytes by remember { mutableStateOf<ByteArray?>(null) }
    var dropBytes by remember { mutableStateOf<ByteArray?>(null) }
    var explodeBytes by remember { mutableStateOf<ByteArray?>(null) }
    var bgmBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(enabled) {
        if (enabled) {
            try {
                moveBytes = Res.readBytes("files/sounds/move.wav")
                dropBytes = Res.readBytes("files/sounds/drop.wav")
                explodeBytes = Res.readBytes("files/sounds/explode.wav")
                bgmBytes = Res.readBytes("files/sounds/bgm.wav")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            moveBytes = null
            dropBytes = null
            explodeBytes = null
            bgmBytes = null
        }
    }

    return rememberNativeSoundEffectPlayer(moveBytes, dropBytes, explodeBytes, bgmBytes)
}

@Composable
expect fun rememberNativeSoundEffectPlayer(
    moveBytes: ByteArray?,
    dropBytes: ByteArray?,
    explodeBytes: ByteArray?,
    bgmBytes: ByteArray?,
): SoundEffectPlayer

