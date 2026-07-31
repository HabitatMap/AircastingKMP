import UIKit
import SwiftUI
import Shared

struct ComposeTab: UIViewControllerRepresentable {
    let tab: AppTab

    func makeUIViewController(context: Context) -> UIViewController {
        TabViewControllerKt.TabViewController(tab: tab)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var selection: AppTab = .home

    var body: some View {
        TabView(selection: $selection) {
            tab(.home,      systemImage: "house")
            tab(.explore,   systemImage: "map")
            tab(.record,    systemImage: "plus.circle")
            tab(.favorites, systemImage: "heart")
            tab(.mydata,    systemImage: "folder")
        }
    }

    private func tab(_ t: AppTab, systemImage: String) -> some View {
        ComposeTab(tab: t)
        .ignoresSafeArea(edges: .top)
        .tabItem { Label(TabTitles.shared.of(tab: t), systemImage: systemImage) }
        .tag(t)
    }
}