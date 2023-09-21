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
            GeometryReader { geometry in
                VStack(spacing: 0) {
                    TabsContainerView()
                    ToolbarView()
                        .frame(height: toolbarHeight(baseOn: geometry.size))
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
    func toolbarHeight(baseOn wndSize: CGSize) -> CGFloat{
        let scale = min(wndSize.width / (maxDragWndWidth - maxDragWndWidth), wndSize.height / (maxDragWndHeight - 60))
        return max(toolBarMinHeight, min(toolBarH, scale * wndSize.height))
    }
}
