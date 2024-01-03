//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by instinct on 2023/12/27.
//

import SwiftUI
import DwebWebBrowser
import DwebShared

@main
struct DwebBrowserApp: App {
    var body: some Scene {
        WindowGroup {
            BrowserView()
                .onAppear {
                    Main_iosKt.doTest()
                    Main_iosKt.doTest()
                }
        }
    }
}

#Preview(body: {
    BrowserView()
})
