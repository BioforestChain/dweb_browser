//
//  AppVersionMgr.swift
//  iosApp
//
//  Created by ui06 on 1/9/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Combine

class AppVersionMgr: ObservableObject {
    @Published var needUpdate = false
    
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
            let (data, _) = try await URLSession.shared.data(from: appInfoUrl)
            let appInfo = try JSONDecoder().decode(AppInfo.self, from: data)
            return appInfo.results.first?.version
        } catch {
            print("Error: \(error.localizedDescription)")
            return nil
        }
    }
    
}
