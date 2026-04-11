import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.container, edges: [.top, .leading, .trailing])
            .safeAreaInset(edge: .bottom, spacing: 0) {
                StackShiftBannerHost()
                    .frame(height: 50)
            }
    }
}



