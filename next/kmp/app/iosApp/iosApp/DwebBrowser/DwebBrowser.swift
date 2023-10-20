//
//  DwebBrowserApp.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI

struct DwebBrowser: View {
    var body: some View {
        ZDeckView()
    }
}

struct ZDeckView: View {
    @State private var zIndexs = Array(repeating: 0, count: 3) // 初始 Z 轴索引
    @State private var maxZindex = 0 // 初始 Z 轴索引
    @State var wndOffset: CGSize = .zero

    @State private var isPrested = false

    var body: some View {
        ZStack {
            Color.green.ignoresSafeArea()
            Color.white

            ForEach(0 ..< zIndexs.count) { i in
                ZStack {
                    BrowserView()
                        .resizableSheet(isPresented: $isPrested) {
                            MySheetView()
                        }
                        .windowify()
                        .offset(x: CGFloat(i) * 50.0, y: 50.0 * CGFloat(i))

                        .overlay {
                            Button("sheet") {
                                isPrested.toggle()
                            }
                            .offset(y: -50)
                        }
                }
            }
        }
    }
}
