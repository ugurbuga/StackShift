package com.ugurbuga.blockgames.platform.feedback

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
actual fun rememberNativeSoundEffectPlayer(
    moveBytes: ByteArray?,
    dropBytes: ByteArray?,
    explodeBytes: ByteArray?,
    bgmBytes: ByteArray?
): SoundEffectPlayer {
    val context = LocalContext.current
    val soundPlayer = remember { AndroidSoundPlayer() }

    LaunchedEffect(moveBytes, dropBytes, explodeBytes, bgmBytes) {
        if (moveBytes != null && soundPlayer.moveSoundId == null) {
            val f = File.createTempFile("move", ".wav", context.cacheDir)
            f.writeBytes(moveBytes)
            soundPlayer.moveSoundId = soundPlayer.soundPool.load(f.absolutePath, 1)
        }
        if (dropBytes != null && soundPlayer.dropSoundId == null) {
            val f = File.createTempFile("drop", ".wav", context.cacheDir)
            f.writeBytes(dropBytes)
            soundPlayer.dropSoundId = soundPlayer.soundPool.load(f.absolutePath, 1)
        }
        if (explodeBytes != null && soundPlayer.explodeSoundId == null) {
            val f = File.createTempFile("explode", ".wav", context.cacheDir)
            f.writeBytes(explodeBytes)
            soundPlayer.explodeSoundId = soundPlayer.soundPool.load(f.absolutePath, 1)
        }
        if (bgmBytes != null && soundPlayer.bgmPlayer == null) {
            val f = File.createTempFile("bgm", ".wav", context.cacheDir)
            f.writeBytes(bgmBytes)
            val mp = MediaPlayer()
            mp.setDataSource(f.absolutePath)
            mp.isLooping = true
            mp.setVolume(0.4f, 0.4f)
            mp.prepare()
            soundPlayer.bgmPlayer = mp
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    return soundPlayer
}

class AndroidSoundPlayer : SoundEffectPlayer {
    val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    var moveSoundId: Int? = null
    var dropSoundId: Int? = null
    var explodeSoundId: Int? = null
    var bgmPlayer: MediaPlayer? = null
    var dragLoopStreamId: Int? = null

    private var isDragLoopPending = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == moveSoundId && isDragLoopPending) {
                startDragLoop()
            }
        }
    }

    override fun play(effect: GameSound) {
        when (effect) {
            GameSound.Grab -> moveSoundId?.let { soundPool.play(it, 0.8f, 0.8f, 1, 0, 1.2f) }
            GameSound.DropSuccess, GameSound.DropInvalid -> dropSoundId?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
            GameSound.LineClear -> explodeSoundId?.let { soundPool.play(it, 1f, 1f, 2, 0, 1f) }
            GameSound.Combo -> explodeSoundId?.let { soundPool.play(it, 0.9f, 0.9f, 1, 0, 1.1f) }
            GameSound.PerfectDrop -> moveSoundId?.let { soundPool.play(it, 0.7f, 0.7f, 1, 0, 1.5f) }
            GameSound.DragLoop -> {
                isDragLoopPending = true
                startDragLoop()
            }
            GameSound.Bgm -> {
                bgmPlayer?.let {
                    if (!it.isPlaying) {
                        it.start()
                    }
                }
            }
            else -> {}
        }
    }

    private fun startDragLoop() {
        if (dragLoopStreamId == null) {
            moveSoundId?.let {
                dragLoopStreamId = soundPool.play(it, 0.35f, 0.35f, 1, -1, 0.7f)
                if (dragLoopStreamId != 0) {
                    isDragLoopPending = false
                } else {
                    dragLoopStreamId = null
                }
            }
        }
    }

    override fun stop(effect: GameSound) {
        when (effect) {
            GameSound.DragLoop -> {
                isDragLoopPending = false
                dragLoopStreamId?.let {
                    soundPool.stop(it)
                    dragLoopStreamId = null
                }
            }
            GameSound.Bgm -> {
                bgmPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                    }
                }
            }
            else -> {}
        }
    }

    fun release() {
        soundPool.release()
        bgmPlayer?.release()
    }
}
