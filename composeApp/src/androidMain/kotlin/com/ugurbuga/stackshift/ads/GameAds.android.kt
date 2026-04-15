package com.ugurbuga.stackshift.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.ugurbuga.stackshift.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.util.Log
import kotlinx.coroutines.delay

private const val BannerRefreshIntervalMillis = 60_000L

@Composable
actual fun rememberPlatformGameAdController(): GameAdController {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val inspectionMode = LocalInspectionMode.current
    val controller = remember(applicationContext) {
        AndroidGameAdController(
            context = applicationContext,
            activityProvider = { context.findActivity() },
        )
    }

    DisposableEffect(controller, inspectionMode) {
        if (!inspectionMode) {
            controller.preloadAds()
        }
        onDispose { }
    }

    return controller
}

private class AndroidGameAdController(
    private val context: Context,
    private val activityProvider: () -> Activity?,
) : GameAdController {
    private var mobileAdsInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun preloadAds() {
        try {
            initializeMobileAdsIfNeeded()
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to initialize MobileAds", e)
            return
        }
        try {
            if (interstitialAd == null) {
                loadInterstitial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to load interstitial ad", e)
        }
        try {
            if (rewardedAd == null) {
                loadRewarded()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to load rewarded ad", e)
        }
    }

    override fun showRestartInterstitial(onFinished: () -> Unit) {
        try {
            preloadAds()
            val activity = activityProvider()
            val ad = interstitialAd
            if (activity == null || ad == null) {
                onFinished()
                loadInterstitial()
                return
            }
            interstitialAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onFinished()
                    loadInterstitial()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    onFinished()
                    loadInterstitial()
                }
            }
            try {
                ad.show(activity)
            } catch (e: Exception) {
                Log.e("GameAds", "Failed to show interstitial ad", e)
                onFinished()
                loadInterstitial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in showRestartInterstitial", e)
            onFinished()
        }
    }

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) {
        try {
            preloadAds()
            val activity = activityProvider()
            val ad = rewardedAd
            if (activity == null || ad == null) {
                onResult(false)
                loadRewarded()
                return
            }
            rewardedAd = null
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onResult(rewardEarned)
                    loadRewarded()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    onResult(false)
                    loadRewarded()
                }
            }
            try {
                ad.show(activity) {
                    rewardEarned = true
                }
            } catch (e: Exception) {
                Log.e("GameAds", "Failed to show rewarded ad", e)
                onResult(false)
                loadRewarded()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in showRewardedRevive", e)
            onResult(false)
        }
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        val inspectionMode = LocalInspectionMode.current
        if (inspectionMode) return

        var bannerAdView by remember { mutableStateOf<AdView?>(null) }

        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                AdView(viewContext).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AndroidAdMobIds.BannerAdUnitId
                }
            },
            update = { view ->
                if (bannerAdView !== view) {
                    bannerAdView = view
                }
            },
        )

        val currentBannerAdView = bannerAdView ?: return

        LaunchedEffect(currentBannerAdView) {
            refreshBannerAd(currentBannerAdView)
            while (true) {
                delay(BannerRefreshIntervalMillis)
                refreshBannerAd(currentBannerAdView)
            }
        }

        DisposableEffect(currentBannerAdView) {
            onDispose {
                currentBannerAdView.destroy()
                bannerAdView = null
            }
        }
    }

    private fun initializeMobileAdsIfNeeded() {
        if (mobileAdsInitialized) return
        try {
            MobileAds.initialize(context)
            mobileAdsInitialized = true
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in MobileAds.initialize", e)
        }
    }

    private fun loadInterstitial() {
        try {
            InterstitialAd.load(
                context,
                AndroidAdMobIds.InterstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        Log.e("GameAds", "Interstitial ad failed to load: $loadAdError")
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in loadInterstitial", e)
        }
    }

    private fun loadRewarded() {
        try {
            RewardedAd.load(
                context,
                AndroidAdMobIds.RewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        Log.e("GameAds", "Rewarded ad failed to load: $loadAdError")
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in loadRewarded", e)
        }
    }

    private fun refreshBannerAd(adView: AdView) {
        try {
            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in refreshBannerAd", e)
        }
    }
}

private object AndroidAdMobIds {
    val BannerAdUnitId: String = BuildConfig.ADS_BANNER_UNIT_ID
    val InterstitialAdUnitId: String = BuildConfig.ADS_INTERSTITIAL_UNIT_ID
    val RewardedAdUnitId: String = BuildConfig.ADS_REWARDED_UNIT_ID
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

