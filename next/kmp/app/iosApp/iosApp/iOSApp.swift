import Network
import SwiftUI

enum RenderType {
    case none
    case webOS
    case deskOS
}

let renderType = RenderType.deskOS

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(DwebAppDelegate.self) var appDelegate
    @StateObject private var networkManager = NetworkManager()
    @State private var isNetworkSegmentViewPresented = false
    @ObservedObject private var deskVCStore = DwebDeskVCStore.shared

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
            switch renderType {
            case .webOS:
                DWebOS()
            case .deskOS:
                DwebFrameworkContentView(vcs: $deskVCStore.vcs)
                    .ignoresSafeArea(.all, edges: .all)
            default:
                DwebBrowser()
            }
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
