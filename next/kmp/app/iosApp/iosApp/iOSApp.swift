import DwebShared
import Network
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(DwebAppDelegate.self) var appDelegate
    @StateObject private var networkManager = NetworkManager()
    @StateObject private var versionMgr = AppVersionMgr()
    @ObservedObject private var deskVCStore = DwebDeskVCStore.shared
    @State private var isNetworkSegmentViewPresented = false

    var body: some Scene {
        WindowGroup {
            content
                .sheet(isPresented: $isNetworkSegmentViewPresented) {
                    NetworkGuidView()
                }
                .onReceive(networkManager.$isNetworkAvailable) { isAvailable in
                    isNetworkSegmentViewPresented = !isAvailable
                }
        }
    }

    var content: some View {
        ZStack(alignment: .center, content: {
            DwebFrameworkContentView(vcs: $deskVCStore.vcs)
                .ignoresSafeArea(.all, edges: .all)
                .persistentSystemOverlays(DwebDeskVCStore.shared.navgationBarVisible)
        })
        .alert(isPresented: $versionMgr.needUpdate) {
            Alert(title: Text("更新提示"), 
                  message: Text("有新版本可用，请您前往App Store更新。"),
                  primaryButton: .cancel(),
                  secondaryButton: .default(Text("前往更新"), action: {
                UIApplication.shared.open(URL(string: appStoreURL)!, options: [:], completionHandler: nil)
            }))
        }
        .task {
            await versionMgr.checkUpdate()
        }
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
