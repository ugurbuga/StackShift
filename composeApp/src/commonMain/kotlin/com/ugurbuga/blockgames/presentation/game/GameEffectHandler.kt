package com.ugurbuga.blockgames.presentation.game

import com.ugurbuga.blockgames.game.model.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class GameEffectHandler(
    private val scope: CoroutineScope,
    private val stateProvider: () -> GameState,
    private val dispatchIntent: (GameIntent) -> Unit,
) {
    private var softLockJob: Job? = null

    fun handle(effect: GameEffect) {
        when (effect) {
            GameEffect.CancelSoftLockTimer -> {
                softLockJob?.cancel()
                softLockJob = null
            }

            is GameEffect.StartSoftLockTimer -> {
                softLockJob?.cancel()
                softLockJob = scope.launch {
                    delay(effect.delayMillis.milliseconds)
                    val latest = stateProvider().softLock
                    if (latest?.revision == effect.revision) {
                        dispatchIntent(GameIntent.CommitSoftLock)
                    }
                }
            }
        }
    }

    fun dispose() {
        softLockJob?.cancel()
        softLockJob = null
    }
}

