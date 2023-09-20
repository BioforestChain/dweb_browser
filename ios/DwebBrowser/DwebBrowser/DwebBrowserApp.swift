//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import Network
import SwiftUI

@main
struct DwebBrowserApp: App {
    @StateObject private var networkManager = NetworkManager()
    @State private var isNetworkSegmentViewPresented = false

    var body: some Scene {
        WindowGroup {
            ZDeckView()
                .sheet(isPresented: $isNetworkSegmentViewPresented) {
                    NetworkGuidView()
                }
                .onReceive(networkManager.$isNetworkAvailable) { isAvailable in
                    isNetworkSegmentViewPresented = !isAvailable
                }
        }
    }
}
