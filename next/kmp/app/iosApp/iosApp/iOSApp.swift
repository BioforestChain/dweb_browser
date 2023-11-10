import SwiftUI
import WebRTC
import Network

let DWEB_OS = true

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(DwebAppDelegate.self) var appDelegate
    @StateObject private var networkManager = NetworkManager()
    @State private var isNetworkSegmentViewPresented = false
    
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
		}
	}
}
