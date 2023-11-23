import Network
import SwiftUI

let DWEB_OS = true
let DWEB_DESK = true

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(DwebAppDelegate.self) var appDelegate
    @StateObject private var networkManager = NetworkManager()
    @State private var isNetworkSegmentViewPresented = false
    @ObservedObject private var deskVCStore = DwebDeskVCStroe.shared

    var body: some Scene {
        WindowGroup {
            if DWEB_DESK {
                DwebFrameworkContentView(vcs: $deskVCStore.vcs).ignoresSafeArea(.all, edges: .all)
            } else {
                ZStack {
                    if DWEB_OS {
                        DWebOS()
                    } else {
                        DwebBrowser()
                    }
                }
                .sheet(isPresented: $isNetworkSegmentViewPresented) {
                    NetworkGuidView()
                }
                .onReceive(networkManager.$isNetworkAvailable) { isAvailable in
                    isNetworkSegmentViewPresented = !isAvailable
                }
            }
        }
    }
}

struct DwebFrameworkContentView: View {
    @Binding var vcs: [Int32: DwebPureViewController]
    var body: some View {
        ZStack {
            if vcs.isEmpty {
                Text("Loading...")
            } else {
                ForEach(Array(vcs.values), id: \.prop.vcId) { pureVc in
                    if pureVc.prop.visible {
                        CommonVCWrapView(vc: pureVc.vc, prop: pureVc.prop).zIndex(Double(pureVc.prop.zIndex))
                    }
                }
            }
        }
    }
}
