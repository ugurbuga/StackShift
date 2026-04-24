package com.ugurbuga.stackshift.platform.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toDataUri(): String {
    val base64 = Base64.encode(this)
    return "data:audio/wav;base64,$base64"
}

fun createAudio(url: String): JsAny = js("new Audio(url)")
fun playAudio(audio: JsAny): Unit = js("audio.play().catch(function(){})")
fun pauseAudio(audio: JsAny): Unit = js("audio.pause()")
fun resetAudio(audio: JsAny): Unit = js("audio.currentTime = 0")
fun loopAudio(audio: JsAny): Unit = js("audio.loop = true")

@Composable
actual fun rememberNativeSoundEffectPlayer(
    moveBytes: ByteArray?,
    dropBytes: ByteArray?,
    explodeBytes: ByteArray?,
    bgmBytes: ByteArray?
): SoundEffectPlayer {
    val soundPlayer = remember { WasmSoundPlayer() }

    LaunchedEffect(moveBytes, dropBytes, explodeBytes, bgmBytes) {
        if (moveBytes != null && soundPlayer.moveAudio == null) {
            val uri = moveBytes.toDataUri()
            soundPlayer.moveAudio = createAudio(uri)
            val dragLoop = createAudio(uri)
            loopAudio(dragLoop)
            soundPlayer.dragLoopAudio = dragLoop
            soundPlayer.perfectDropAudio = createAudio(uri)
        }
        if (dropBytes != null && soundPlayer.dropAudio == null) {
            soundPlayer.dropAudio = createAudio(dropBytes.toDataUri())
        }
        if (explodeBytes != null && soundPlayer.explodeAudio == null) {
            soundPlayer.explodeAudio = createAudio(explodeBytes.toDataUri())
        }
        if (bgmBytes != null && soundPlayer.bgmAudio == null) {
            val audio = createAudio(bgmBytes.toDataUri())
            loopAudio(audio)
            soundPlayer.bgmAudio = audio
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    return soundPlayer
}

class WasmSoundPlayer : SoundEffectPlayer {
    var moveAudio: JsAny? = null
    var dropAudio: JsAny? = null
    var explodeAudio: JsAny? = null
    var bgmAudio: JsAny? = null
    var dragLoopAudio: JsAny? = null
    var perfectDropAudio: JsAny? = null

    override fun play(effect: GameSound) {
        when (effect) {
            GameSound.Grab -> {
                moveAudio?.let {
                    resetAudio(it)
                    playAudio(it)
                }
            }
            GameSound.DropSuccess, GameSound.DropInvalid -> {
                dropAudio?.let {
                    resetAudio(it)
                    playAudio(it)
                }
            }
            GameSound.LineClear, GameSound.Combo -> {
                explodeAudio?.let {
                    resetAudio(it)
                    playAudio(it)
                }
            }
            GameSound.PerfectDrop -> {
                perfectDropAudio?.let {
                    resetAudio(it)
                    playAudio(it)
                }
            }
            GameSound.DragLoop -> {
                dragLoopAudio?.let {
                    playAudio(it)
                }
            }
            GameSound.Bgm -> {
                bgmAudio?.let {
                    playAudio(it)
                }
            }
            else -> {}
        }
    }

    override fun stop(effect: GameSound) {
        when (effect) {
            GameSound.DragLoop -> {
                dragLoopAudio?.let {
                    pauseAudio(it)
                    resetAudio(it)
                }
            }
            GameSound.Bgm -> {
                bgmAudio?.let { pauseAudio(it) }
            }
            else -> {}
        }
    }

    fun release() {
        bgmAudio?.let { pauseAudio(it) }
        dragLoopAudio?.let { pauseAudio(it) }
        perfectDropAudio?.let { pauseAudio(it) }
    }
}
