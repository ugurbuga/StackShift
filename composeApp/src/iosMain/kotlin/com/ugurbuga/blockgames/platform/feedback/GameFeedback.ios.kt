package com.ugurbuga.blockgames.platform.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
    }
}

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberNativeSoundEffectPlayer(
    moveBytes: ByteArray?,
    dropBytes: ByteArray?,
    explodeBytes: ByteArray?,
    bgmBytes: ByteArray?
): SoundEffectPlayer {
    val soundPlayer = remember { IosSoundPlayer() }

    LaunchedEffect(moveBytes, dropBytes, explodeBytes, bgmBytes) {
        if (moveBytes != null && soundPlayer.movePlayer == null) {
            val data = moveBytes.toNSData()
            soundPlayer.movePlayer = AVAudioPlayer(data, null)
            soundPlayer.movePlayer?.prepareToPlay()
            soundPlayer.dragLoopPlayer = AVAudioPlayer(data, null).apply {
                numberOfLoops = -1
                volume = 0.5f
                prepareToPlay()
            }
            soundPlayer.perfectDropPlayer = AVAudioPlayer(data, null).apply {
                volume = 0.6f
                prepareToPlay()
            }
        }
        if (dropBytes != null && soundPlayer.dropPlayer == null) {
            soundPlayer.dropPlayer = AVAudioPlayer(dropBytes.toNSData(), null)
            soundPlayer.dropPlayer?.prepareToPlay()
        }
        if (explodeBytes != null && soundPlayer.explodePlayer == null) {
            soundPlayer.explodePlayer = AVAudioPlayer(explodeBytes.toNSData(), null)
            soundPlayer.explodePlayer?.prepareToPlay()
        }
        if (bgmBytes != null && soundPlayer.bgmPlayer == null) {
            soundPlayer.bgmPlayer = AVAudioPlayer(bgmBytes.toNSData(), null)?.apply {
                numberOfLoops = -1
                volume = 0.4f
                prepareToPlay()
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

class IosSoundPlayer : SoundEffectPlayer {
    var movePlayer: AVAudioPlayer? = null
    var dropPlayer: AVAudioPlayer? = null
    var explodePlayer: AVAudioPlayer? = null
    var bgmPlayer: AVAudioPlayer? = null
    var dragLoopPlayer: AVAudioPlayer? = null
    var perfectDropPlayer: AVAudioPlayer? = null

    override fun play(effect: GameSound) {
        when (effect) {
            GameSound.Grab -> {
                movePlayer?.let {
                    it.currentTime = 0.0
                    it.play()
                }
            }
            GameSound.DropSuccess, GameSound.DropInvalid -> {
                dropPlayer?.let {
                    it.currentTime = 0.0
                    it.play()
                }
            }
            GameSound.LineClear, GameSound.Combo -> {
                explodePlayer?.let {
                    it.currentTime = 0.0
                    it.play()
                }
            }
            GameSound.PerfectDrop -> {
                perfectDropPlayer?.let {
                    it.currentTime = 0.0
                    it.play()
                }
            }
            GameSound.DragLoop -> {
                dragLoopPlayer?.play()
            }
            GameSound.Bgm -> {
                bgmPlayer?.play()
            }
            else -> {}
        }
    }

    override fun stop(effect: GameSound) {
        when (effect) {
            GameSound.DragLoop -> {
                dragLoopPlayer?.stop()
                dragLoopPlayer?.currentTime = 0.0
            }
            GameSound.Bgm -> bgmPlayer?.pause()
            else -> {}
        }
    }

    fun release() {
        bgmPlayer?.stop()
        dragLoopPlayer?.stop()
    }
}
