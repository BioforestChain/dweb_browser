//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
import Network

@main
struct DwebBrowserApp: App {
    @StateObject private var networkManager = NetworkManager()

    var body: some Scene {
        WindowGroup {
            BrowserView()
                .environmentObject(networkManager)
                .environment(\.colorScheme, ColorScheme.light)
        }
    }
}
