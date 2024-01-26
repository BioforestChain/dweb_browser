//
//  AppVersionMgr.swift
//  DwebBrowser
//
//  Created by ui06 on 1/9/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Observation

@Observable class AppVersionMgr {
    var needUpdate = false
    
    static let appStoreURL = "itms-apps://itunes.apple.com/app/id6443558874"
    static let appInfoUrl = URL(string: "https://itunes.apple.com/lookup?id=6443558874")!
    
    func checkUpdate() async {
        let appstoreVersion = await fetchAppVersion()
        let localVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        if let remoteV = appstoreVersion, let localV = localVersion, remoteV > localV {
            DispatchQueue.main.async {
                self.needUpdate = true
            }
        }
    }

    struct AppInfo: Codable {
        let results: [Result]
        struct Result: Codable {
            let version: String
        }
    }

    func fetchAppVersion() async -> String? {
        do {
            let (data, _) = try await URLSession.shared.data(from: AppVersionMgr.appInfoUrl)
            let appInfo = try JSONDecoder().decode(AppInfo.self, from: data)
            return appInfo.results.first?.version
        } catch {
            print("Error: \(error.localizedDescription)")
            return nil
        }
    }
    
}
