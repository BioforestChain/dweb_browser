import Network
import SwiftUI
import DwebShared

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
    @State var webContainerSize: CGSize = CGSize(width: 350, height: 500)

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
        .task {
            let v = UIHostingController(rootView: BrowserView(size: $webContainerSize)).view!
            Main_iosKt.regiserIosMainView(iosView: v)
            Main_iosKt.registerIosOnSize { (w,h) in
                webContainerSize.width = CGFloat(truncating: w)
                webContainerSize.height = CGFloat(truncating: h)
            }
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
