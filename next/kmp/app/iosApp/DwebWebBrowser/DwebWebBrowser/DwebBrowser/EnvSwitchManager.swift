//
//  EnvSwitchManager.swift
//  DwebWebBrowser
//
//  Created by linjie on 2024/7/30.
//

import Foundation

struct AppConfig: Codable {
    var isWebTopMenuEnabled: Bool
}

class EnvSwitchManager: ObservableObject {
    @Published var config: AppConfig?

    init() {
        loadConfig()
    }

    func loadConfig() {
        guard let url = Bundle.main.url(forResource: "envSwitch", withExtension: "json") else {
            print("Config file not found")
            return
        }

        do {
            let data = try Data(contentsOf: url)
            let decoder = JSONDecoder()
            config = try decoder.decode(AppConfig.self, from: data)
        } catch {
            print("Error loading config: \(error)")
        }
    }
}
