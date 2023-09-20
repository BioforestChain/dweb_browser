//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct BrowserView: View {
    @StateObject var selectedTab = SelectedTab()
    @StateObject var addressBar = AddressBarState()
    @StateObject var openingLink = OpeningLink()
    @StateObject var toolBarState = ToolBarState()
    @StateObject var webcacheStore = WebCacheStore()

    var body: some View {
        ZStack {
            GeometryReader { _ in
                VStack(spacing: 0) {
                    TabsContainerView()
//                    ToolbarView()
                }
                .background(Color.bkColor)
                .environmentObject(webcacheStore)
                .environmentObject(openingLink)
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
                .environmentObject(toolBarState)
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
                        .resizableSheet(isPresented: $isPrested, content: {
                            ZStack {
                                VStack {
                                    Text("this is sheet view")
                                    Spacer()

                                    HStack {
                                        Text("leading bottom")
                                        Spacer()
                                        Text("trealing bottom")
                                    }
                                }
                            }
                            .background(.green)
                        })
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


struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        ZDeckView()
    }
}
