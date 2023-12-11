import DwebShared
import Network
import SwiftUI

enum RenderType {
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
    @State private var showAlert = false

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
                    .persistentSystemOverlays(DwebDeskVCStore.shared.navgationBarVisible)
            
            }
        })
        .alert(isPresented: $showAlert) {
            Alert(title: Text("更新提示"), message: Text("有新版本可用，请您前往App Store更新。"), primaryButton: .cancel(), secondaryButton: .default(Text("前往更新"), action: {
                let appStoreURL = "itms-apps://itunes.apple.com/app/id6443558874"
                UIApplication.shared.open(URL(string: appStoreURL)!, options: [:], completionHandler: nil)
            }))
        }
        .task {
            await checkUpdate()
        }
    }

    func checkUpdate() async {
        let appstoreVersion = await fetchAppVersion()
        let localVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        if let remoteV = appstoreVersion, let localV = localVersion, remoteV > localV {
            showAlert = true
        }
    }

    struct AppInfo: Codable {
        let results: [Result]
        struct Result: Codable {
            let version: String
        }
    }

    func fetchAppVersion() async -> String? {
        let url = URL(string: "https://itunes.apple.com/lookup?id=6443558874")!
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let appInfo = try JSONDecoder().decode(AppInfo.self, from: data)
            return appInfo.results.first?.version
        } catch {
            print("Error: \(error.localizedDescription)")
            return nil
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
