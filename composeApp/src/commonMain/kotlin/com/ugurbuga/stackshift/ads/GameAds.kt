package com.ugurbuga.stackshift.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface GameAdController {
    fun showRestartInterstitial(onFinished: () -> Unit)

    fun showRewardedRevive(onResult: (Boolean) -> Unit)

    @Composable
    fun Banner(modifier: Modifier = Modifier)
}

object NoOpGameAdController : GameAdController {
    override fun showRestartInterstitial(onFinished: () -> Unit) = onFinished()

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) = onResult(false)

    @Composable
    override fun Banner(modifier: Modifier) = Unit
}

@Composable
expect fun rememberPlatformGameAdController(): GameAdController

