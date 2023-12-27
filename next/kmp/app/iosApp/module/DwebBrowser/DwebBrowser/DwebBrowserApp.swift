//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by instinct on 2023/12/27.
//

import SwiftUI
import DwebWebBrowser

@main
struct DwebBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            BrowserView()
        }
    }
}

#Preview(body: {
    BrowserView()
})
