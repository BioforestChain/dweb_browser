import SwiftUI
import WebRTC
import Network

let DWEB_OS = true

@main
struct iOSApp: App {
    @StateObject private var networkManager = NetworkManager()
    @State private var isNetworkSegmentViewPresented = false
    
    init() {
        print("iOS init")
        KmpBridgeManager.shared.registerIMPs()
    }
    
	var body: some Scene {
		WindowGroup {
            ZStack{
                if DWEB_OS {
                    DWebOS()
                }else{
                    DwebBrowser()
                }
            }
            .sheet(isPresented: $isNetworkSegmentViewPresented) {
                NetworkGuidView()
            }
            .onReceive(networkManager.$isNetworkAvailable) { isAvailable in
                isNetworkSegmentViewPresented = !isAvailable
            }
            .onReceive(KmpBridgeManager.shared.eventPublisher.filter{ $0.name == KmpEvent.share }, perform: { event in
                event.responseAction?.doResponseAction()
            })
		}
	}
}
