package com.ugurbuga.stackshift.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
interface GameAdController {
    val bannerPresentationMode: BannerPresentationMode

    fun showRestartInterstitial(onFinished: () -> Unit)

    fun showRewardedRevive(onResult: (Boolean) -> Unit)

    @Composable
    fun Banner(
        modifier: Modifier = Modifier,
        onLoadStateChanged: (Boolean) -> Unit = {},
    )
}

enum class BannerPresentationMode {
    Inline,
    External,
}

object NoOpGameAdController : GameAdController {
    override val bannerPresentationMode: BannerPresentationMode = BannerPresentationMode.Inline

    override fun showRestartInterstitial(onFinished: () -> Unit) = onFinished()

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) = onResult(false)

    @Composable
    override fun Banner(modifier: Modifier, onLoadStateChanged: (Boolean) -> Unit) = Unit
}

@Composable
expect fun rememberPlatformGameAdController(): GameAdController

