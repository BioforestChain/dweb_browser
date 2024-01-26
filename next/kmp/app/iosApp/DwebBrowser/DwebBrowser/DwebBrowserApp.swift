import DwebShared
import Network
import SwiftUI

@main
struct DwebBrowserApp: App {
    @UIApplicationDelegateAdaptor(DwebBrowserAppDelegate.self) var appDelegate
    @State private var deskVCStore = DwebDeskVCStore()

    var body: some Scene {
        WindowGroup {
            content
                .deskAlertTip()
        }
    }

    var content: some View {
        ZStack(alignment: .center, content: {
            DwebFrameworkContentView(vcs: $deskVCStore.vcs)
                .ignoresSafeArea(.all, edges: .all)
                .persistentSystemOverlays(deskVCStore.navgationBarVisible)
        })
    }
}

struct DwebFrameworkContentView: View {
    @Binding var vcs: [DwebVCData]
    var body: some View {
        ZStack {
            if vcs.isEmpty {
                Text("Loading...")
            } else {
                DwebDeskRootView(vcs: vcs.map { $0.vc })
            }
        }
    }
}
