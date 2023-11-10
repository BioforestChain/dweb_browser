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
    
    private let maxWidth: CGFloat = 350
    private let maxHeight: CGFloat = 430

    @Binding var size: CGSize
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black
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
            .frame(width: size.width > maxWidth ? maxWidth : size.width, height: size.height > maxHeight ? maxHeight : size.height)

            .onAppear {
                dragScale.onWidth = (geometry.size.width - 10) / screen_width
            }
            .onChange(of: size) { newSize in
                let width = newSize.width > maxWidth ? maxWidth : newSize.width
                dragScale.onWidth = (width - 10) / screen_width
            }
        }
        .resizableSheet(isPresented: $toolBarState.showMoreMenu) {
            SheetSegmentView(isShowingWeb: showWeb())
                .environmentObject(selectedTab)
                .environmentObject(openingLink)
                .environmentObject(webcacheStore)
                .environmentObject(dragScale)
                .environmentObject(toolBarState)
        }
    }

    func showWeb() -> Bool {
        if webcacheStore.caches.count == 0 {
            return true
        }
        return webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb
    }
}
