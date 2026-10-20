import SwiftUI
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinIosKt.startKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { phase in
            // Last chance to run before suspension: drain pending uploads in a background task.
            if phase == .background { KoinIosKt.scheduleUploadBackstop() }
        }
    }
}
