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
    @StateObject var dragScale = WndDragScale()

    @Binding var size: CGSize
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                VStack(spacing: 0) {
                    TabsContainerView()
                    ToolbarView()
                        .frame(height: dragScale.toolbarHeight)
                        .background(Color.bkColor)
                }
                .background(Color.bkColor)
                .environmentObject(webcacheStore)
                .environmentObject(openingLink)
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
                .environmentObject(toolBarState)
                .environmentObject(dragScale)
            }
            .frame(width: size.width, height: size.height)
            .onAppear {
                dragScale.onWidth = (geometry.size.width - 10) / screen_width
            }
            .onChange(of: size) { newSize in
                dragScale.onWidth = (newSize.width - 10) / screen_width
            }
        }
    }
}
