import Foundation
import GoogleMobileAds
import SwiftUI
import UIKit

private enum StackShiftAdNotification {
    static let showInterstitial = Notification.Name("StackShiftAdShowInterstitial")
    static let interstitialCompleted = Notification.Name("StackShiftAdInterstitialCompleted")
    static let showRewarded = Notification.Name("StackShiftAdShowRewardedRevive")
    static let rewardedCompleted = Notification.Name("StackShiftAdRewardedCompleted")
}

private struct StackShiftAdsProperties {
    static let shared = load()

    let iosApplicationId: String
    let banner: String
    let interstitial: String
    let rewarded: String

    private static func load() -> StackShiftAdsProperties {
        guard let url = Bundle.main.url(forResource: "ads", withExtension: "properties"),
              let content = try? String(contentsOf: url, encoding: .utf8) else {
            return StackShiftAdsProperties(
                iosApplicationId: "",
                banner: "",
                interstitial: "",
                rewarded: ""
            )
        }

        let values = parse(content)
        return StackShiftAdsProperties(
            iosApplicationId: values["ads.ios.applicationId"] ?? "",
            banner: values["ads.ios.bannerUnitId"] ?? "",
            interstitial: values["ads.ios.interstitialUnitId"] ?? "",
            rewarded: values["ads.ios.rewardedUnitId"] ?? ""
        )
    }

    private static func parse(_ content: String) -> [String: String] {
        var values: [String: String] = [:]

        for line in content.components(separatedBy: .newlines) {
            let trimmedLine = line.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmedLine.isEmpty, !trimmedLine.hasPrefix("#"), let separatorIndex = trimmedLine.firstIndex(of: "=") else {
                continue
            }

            let key = String(trimmedLine[..<separatorIndex]).trimmingCharacters(in: .whitespaces)
            let value = String(trimmedLine[trimmedLine.index(after: separatorIndex)...]).trimmingCharacters(in: .whitespaces)
            values[key] = value
        }

        return values
    }
}

private enum StackShiftAdMobIDs {
    static let banner = StackShiftAdsProperties.shared.banner
    static let interstitial = StackShiftAdsProperties.shared.interstitial
    static let rewarded = StackShiftAdsProperties.shared.rewarded
}

final class StackShiftAdBridge: NSObject, FullScreenContentDelegate {
    static let shared = StackShiftAdBridge()

    private var started = false
    private var observerTokens: [NSObjectProtocol] = []
    private var interstitialAd: InterstitialAd?
    private var rewardedAd: RewardedAd?
    private var pendingInterstitialRequestId: String?
    private var pendingRewardedRequestId: String?
    private var didEarnReward = false

    private override init() {
        super.init()
    }

    func start() {
        guard !started else { return }
        started = true

        MobileAds.shared.start(completionHandler: nil)
        registerObservers()
        loadInterstitial()
        loadRewarded()
    }

    deinit {
        observerTokens.forEach(NotificationCenter.default.removeObserver)
    }

    private func registerObservers() {
        let center = NotificationCenter.default

        observerTokens.append(
            center.addObserver(
                forName: StackShiftAdNotification.showInterstitial,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                self?.handleInterstitialRequest(notification)
            }
        )

        observerTokens.append(
            center.addObserver(
                forName: StackShiftAdNotification.showRewarded,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                self?.handleRewardedRequest(notification)
            }
        )
    }

    private func handleInterstitialRequest(_ notification: Notification) {
        guard let requestId = notification.object as? String, !requestId.isEmpty else { return }
        guard let ad = interstitialAd, let rootViewController = Self.topViewController() else {
            postInterstitialCompleted(requestId: requestId)
            loadInterstitial()
            return
        }

        interstitialAd = nil
        pendingInterstitialRequestId = requestId
        ad.fullScreenContentDelegate = self
        ad.present(from: rootViewController)
    }

    private func handleRewardedRequest(_ notification: Notification) {
        guard let requestId = notification.object as? String, !requestId.isEmpty else { return }
        guard let ad = rewardedAd, let rootViewController = Self.topViewController() else {
            postRewardedCompleted(requestId: requestId, rewarded: false)
            loadRewarded()
            return
        }

        rewardedAd = nil
        pendingRewardedRequestId = requestId
        didEarnReward = false
        ad.fullScreenContentDelegate = self
        ad.present(from: rootViewController) { [weak self] in
            self?.didEarnReward = true
        }
    }

    private func loadInterstitial() {
        InterstitialAd.load(
            with: StackShiftAdMobIDs.interstitial,
            request: Request()
        ) { [weak self] ad, _ in
            self?.interstitialAd = ad
        }
    }

    private func loadRewarded() {
        RewardedAd.load(
            with: StackShiftAdMobIDs.rewarded,
            request: Request()
        ) { [weak self] ad, _ in
            self?.rewardedAd = ad
        }
    }

    private func finishInterstitialIfNeeded() {
        guard let requestId = pendingInterstitialRequestId else { return }
        pendingInterstitialRequestId = nil
        postInterstitialCompleted(requestId: requestId)
        loadInterstitial()
    }

    private func finishRewardedIfNeeded(rewarded: Bool) {
        guard let requestId = pendingRewardedRequestId else { return }
        pendingRewardedRequestId = nil
        didEarnReward = false
        postRewardedCompleted(requestId: requestId, rewarded: rewarded)
        loadRewarded()
    }

    private func postInterstitialCompleted(requestId: String) {
        NotificationCenter.default.post(name: StackShiftAdNotification.interstitialCompleted, object: requestId)
    }

    private func postRewardedCompleted(requestId: String, rewarded: Bool) {
        NotificationCenter.default.post(name: StackShiftAdNotification.rewardedCompleted, object: "\(requestId):\(rewarded)")
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        if pendingInterstitialRequestId != nil {
            finishInterstitialIfNeeded()
        } else {
            finishRewardedIfNeeded(rewarded: didEarnReward)
        }
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        if pendingInterstitialRequestId != nil {
            finishInterstitialIfNeeded()
        } else {
            finishRewardedIfNeeded(rewarded: false)
        }
    }

    static func topViewController(
        base: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    ) -> UIViewController? {
        if let navigationController = base as? UINavigationController {
            return topViewController(base: navigationController.visibleViewController)
        }
        if let tabBarController = base as? UITabBarController {
            return topViewController(base: tabBarController.selectedViewController)
        }
        if let presentedViewController = base?.presentedViewController {
            return topViewController(base: presentedViewController)
        }
        return base
    }
}

struct StackShiftBannerHost: UIViewRepresentable {
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = .clear

        let bannerView = BannerView()
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        bannerView.adUnitID = StackShiftAdMobIDs.banner
        bannerView.rootViewController = StackShiftAdBridge.topViewController()
        bannerView.delegate = context.coordinator

        container.addSubview(bannerView)
        NSLayoutConstraint.activate([
            bannerView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            bannerView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            bannerView.topAnchor.constraint(equalTo: container.topAnchor),
            bannerView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])

        context.coordinator.bannerView = bannerView
        return container
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        guard let bannerView = context.coordinator.bannerView else { return }

        bannerView.rootViewController = StackShiftAdBridge.topViewController()
        let width = max(floor(uiView.bounds.width), floor(UIScreen.main.bounds.width))
        guard width > 0 else { return }
        guard context.coordinator.loadedWidth != width else { return }

        context.coordinator.loadedWidth = width
        bannerView.adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
        bannerView.load(Request())
    }

    final class Coordinator: NSObject, BannerViewDelegate {
        var loadedWidth: CGFloat = 0
        weak var bannerView: BannerView?
    }
}

