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
                DwebFrameworkContentView(vc: $deskVCStore.vc).ignoresSafeArea(.all, edges: .bottom)
            } else {
                ZStack{
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
    @Binding var vc: DwebRootUIViewController?
    var body: some View {
        ZStack {
            if let vc = vc {
                CommonVCWrapView<DwebRootUIViewController>(vc: vc).ignoresSafeArea(.all, edges: .bottom)
            } else {
                Text("Loading...")
            }
        }
    }
}

