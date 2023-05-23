//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI

class AddressBarOffsetOnX: ObservableObject {
    @Published var offset: CGFloat = 0
}

@main
struct DwebBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            BrowserView()
                .environmentObject(AddressBarOffsetOnX())
        }
    }
}
