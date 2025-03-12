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
            DwebFrameworkContentView()
                .ignoresSafeArea(.all, edges: .all)
                .persistentSystemOverlays(deskVCStore.navigationBarVisible)
                .environment(deskVCStore)
                .onChange(of: deskVCStore.shouldEnableEdgeSwipe) { _, enable in
                    
                }
        })
        
    }
}

struct DwebFrameworkContentView: View {
    @Environment(DwebDeskVCStore.self) var vcdStore
    @State private var isDidAppear = false
    
    var body: some View {
        ZStack {
            if vcdStore.vcs.isEmpty {
                Text("Loading...")
                    .accessibilityLabel("loading")
            } else {
                DwebDeskRootView(deskVCStorex: vcdStore)
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
