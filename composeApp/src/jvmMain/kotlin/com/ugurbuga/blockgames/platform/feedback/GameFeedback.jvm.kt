package com.ugurbuga.blockgames.platform.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

@Composable
actual fun rememberNativeSoundEffectPlayer(
    moveBytes: ByteArray?,
    dropBytes: ByteArray?,
    explodeBytes: ByteArray?,
    bgmBytes: ByteArray?
): SoundEffectPlayer {
    val soundPlayer = remember { JvmSoundPlayer() }

    LaunchedEffect(moveBytes, dropBytes, explodeBytes, bgmBytes) {
        if (moveBytes != null && soundPlayer.moveClip == null) {
            soundPlayer.moveClip = loadClip(moveBytes)
            soundPlayer.dragLoopClip = loadClip(moveBytes)?.apply {
                loop(Clip.LOOP_CONTINUOUSLY)
            }
            soundPlayer.perfectDropClip = loadClip(moveBytes)
        }
        if (dropBytes != null && soundPlayer.dropClip == null) {
            soundPlayer.dropClip = loadClip(dropBytes)
        }
        if (explodeBytes != null && soundPlayer.explodeClip == null) {
            soundPlayer.explodeClip = loadClip(explodeBytes)
        }
        if (bgmBytes != null && soundPlayer.bgmClip == null) {
            soundPlayer.bgmClip = loadClip(bgmBytes)?.apply {
                loop(Clip.LOOP_CONTINUOUSLY)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    return soundPlayer
}

private fun loadClip(bytes: ByteArray): Clip? {
    return try {
        val stream = ByteArrayInputStream(bytes)
        val audioStream = AudioSystem.getAudioInputStream(stream)
        val clip = AudioSystem.getClip()
        clip.open(audioStream)
        clip
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

class JvmSoundPlayer : SoundEffectPlayer {
    var moveClip: Clip? = null
    var dropClip: Clip? = null
    var explodeClip: Clip? = null
    var bgmClip: Clip? = null
    var dragLoopClip: Clip? = null
    var perfectDropClip: Clip? = null

    override fun play(effect: GameSound) {
        when (effect) {
            GameSound.Grab -> {
                moveClip?.let {
                    it.framePosition = 0
                    it.start()
                }
            }
            GameSound.DropSuccess, GameSound.DropInvalid -> {
                dropClip?.let {
                    it.framePosition = 0
                    it.start()
                }
            }
            GameSound.LineClear -> {
                explodeClip?.let {
                    it.framePosition = 0
                    it.start()
                }
            }
            GameSound.Combo -> {
                explodeClip?.let {
                    it.framePosition = 0
                    it.start()
                }
            }
            GameSound.PerfectDrop -> {
                perfectDropClip?.let {
                    it.framePosition = 0
                    it.start()
                }
            }
            GameSound.DragLoop -> {
                dragLoopClip?.start()
            }
            GameSound.Bgm -> {
                bgmClip?.start()
            }
            else -> {}
        }
    }

    override fun stop(effect: GameSound) {
        when (effect) {
            GameSound.DragLoop -> {
                dragLoopClip?.let {
                    it.stop()
                    it.framePosition = 0
                }
            }
            GameSound.Bgm -> bgmClip?.stop()
            else -> {}
        }
    }

    fun release() {
        moveClip?.close()
        dropClip?.close()
        explodeClip?.close()
        bgmClip?.close()
        dragLoopClip?.close()
        perfectDropClip?.close()
    }
}
