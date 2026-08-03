import UIKit
import SwiftUI
import Shared

struct ComposeTab: UIViewControllerRepresentable {
    let tab: AppTab
    let onOpenSettings: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        TabViewControllerKt.TabViewController(tab: tab, onOpenSettings: onOpenSettings)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ComposeSettings: UIViewControllerRepresentable {
    let onClose: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        SettingsViewControllerKt.SettingsViewController(onClose: onClose)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var selection: AppTab = .home
    @State private var showSettings = false

    var body: some View {
        TabView(selection: $selection) {
            tab(.home,      systemImage: "house")
            tab(.explore,   systemImage: "map")
            tab(.record,    systemImage: "plus.circle")
            tab(.favorites, systemImage: "heart")
            tab(.mydata,    systemImage: "folder")
        }
            // fullScreenCover — not a push — is what hides the tab bar, since the design has no
            // bottom bar on Settings. Compose owns navigation *inside* the cover.
        .fullScreenCover(isPresented: $showSettings) {
            ComposeSettings(onClose: { showSettings = false })
                .ignoresSafeArea(edges: .top)
        }
    }

    private func tab(_ t: AppTab, systemImage: String) -> some View {
        ComposeTab(tab: t, onOpenSettings: { showSettings = true })
        .ignoresSafeArea(edges: .top)
        .tabItem { Label(TabTitles.shared.of(tab: t), systemImage: systemImage) }
        .tag(t)
    }
}
