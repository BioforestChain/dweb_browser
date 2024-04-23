import DwebShared
import Network
import SwiftUI

@main
struct DwebBrowserApp: App {
    @UIApplicationDelegateAdaptor(DwebBrowserAppDelegate.self) var appDelegate
    @State private var deskVCStore = DwebDeskVCStore()
    
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                content
            }
            .tint(.black)
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
    @State private var isDidAppear = false
    
    var body: some View {
        ZStack {
            if vcs.isEmpty {
                Text("Loading...")
                    .accessibilityLabel("loading")
            } else {
                DwebDeskRootView(vcs: vcs.map { $0.vc })
            }
        }
        .overlay {
            if isDidAppear {
                Color.clear.deskAlertTip()
            }
        }
        .task {
            DwebLifeStatusCenter.shared.register(.didRended) {
                self.isDidAppear = true
            }
        }
    }
}
