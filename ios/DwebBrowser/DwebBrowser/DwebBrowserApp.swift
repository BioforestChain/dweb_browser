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

struct ZDeckView: View {
    @State private var zIndexs = Array(repeating: 0, count: 1) // 初始 Z 轴索引
    @State private var maxZindex = 0 // 初始 Z 轴索引
    @State var wndOffset: CGSize = .zero

    @State private var isPrested = false

    var body: some View {
        ZStack {
            Color.green.ignoresSafeArea()
            Color.white

            ForEach(0 ..< zIndexs.count) { _ in
                ZStack {
                    BrowserView()
                        .resizableSheet(isPresented: $isPrested) {
                            MySheetView()
                        }
                        .windowify()
                        .overlay {
                            Button("sheet") {
                                isPrested.toggle()
                            }
                            .offset(y:-50)
                        }
                }
            }
        }
    }
}
