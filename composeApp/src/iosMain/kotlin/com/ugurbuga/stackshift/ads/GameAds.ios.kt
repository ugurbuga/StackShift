package com.ugurbuga.stackshift.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter

@Composable
actual fun rememberPlatformGameAdController(): GameAdController {
    val controller = remember { IosGameAdController() }
    DisposableEffect(controller) {
        controller.start()
        onDispose(controller::dispose)
    }
    return controller
}

private class IosGameAdController : GameAdController {
    private val interstitialCallbacks = mutableMapOf<String, () -> Unit>()
    private val rewardedCallbacks = mutableMapOf<String, (Boolean) -> Unit>()
    private var nextRequestId = 0L
    private var started = false
    private var interstitialObserver: Any? = null
    private var rewardedObserver: Any? = null

    fun start() {
        if (started) return
        started = true

        interstitialObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = InterstitialCompletedNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            notification?.extractObjectString()?.let { requestId ->
                interstitialCallbacks.remove(requestId)?.invoke()
            }
        }

        rewardedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = RewardedCompletedNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            val payload = notification?.extractObjectString() ?: return@addObserverForName
            val separatorIndex = payload.lastIndexOf(':')
            if (separatorIndex <= 0) return@addObserverForName
            val requestId = payload.substring(0, separatorIndex)
            val rewarded = payload.substring(separatorIndex + 1).toBooleanStrictOrNull() ?: false
            rewardedCallbacks.remove(requestId)?.invoke(rewarded)
        }
    }

    fun dispose() {
        interstitialObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        rewardedObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        interstitialObserver = null
        rewardedObserver = null
        interstitialCallbacks.clear()
        rewardedCallbacks.clear()
        started = false
    }

    override fun showRestartInterstitial(onFinished: () -> Unit) {
        val requestId = nextRequestId().also { interstitialCallbacks[it] = onFinished }
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = InterstitialRequestNotification,
            `object` = requestId,
            userInfo = null,
        )
    }

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) {
        val requestId = nextRequestId().also { rewardedCallbacks[it] = onResult }
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = RewardedRequestNotification,
            `object` = requestId,
            userInfo = null,
        )
    }

    @Composable
    override fun Banner(modifier: Modifier) = Unit

    private fun nextRequestId(): String = (++nextRequestId).toString()
}

private fun NSNotification.extractObjectString(): String? = `object`?.toString()?.takeIf(String::isNotBlank)

private const val InterstitialRequestNotification = "StackShiftAdShowInterstitial"
private const val InterstitialCompletedNotification = "StackShiftAdInterstitialCompleted"
private const val RewardedRequestNotification = "StackShiftAdShowRewardedRevive"
private const val RewardedCompletedNotification = "StackShiftAdRewardedCompleted"

