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

class TabState: ObservableObject {
    @Published var showingOptions = true
    var addressBarHeight: CGFloat{
        showingOptions ? 0 : addressBarH
    }
}

@main
struct DwebBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            BrowserView()
                .environmentObject(AddressBarOffsetOnX())
                .environmentObject(TabState())
        }
    }
}
