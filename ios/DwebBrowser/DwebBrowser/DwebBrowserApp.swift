//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI

class AddrBarOffset: ObservableObject {
    @Published var onX: CGFloat = 0
}

class TabState: ObservableObject {
    @Published var showTabGrid = true
    var addressBarHeight: CGFloat{
        showTabGrid ? 0 : addressBarH
    }
}

@main
struct DwebBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            BrowserView()
                .environmentObject(AddrBarOffset())
                .environmentObject(TabState())
        }
    }
}
