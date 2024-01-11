//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

public struct BrowserView: View {
    @StateObject var store = BrowserViewStateStore.shared
    @StateObject var selectedTab = BrowserViewStateStore.shared.selectedTab
    @StateObject var addressBar = BrowserViewStateStore.shared.addressBar
    @StateObject var openingLink = BrowserViewStateStore.shared.openingLink
    @StateObject var toolBarState = BrowserViewStateStore.shared.toolBarState
    @StateObject var webcacheStore = BrowserViewStateStore.shared.webcacheStore
    @StateObject var dragScale = BrowserViewStateStore.shared.dragScale
    @StateObject var wndArea = BrowserViewStateStore.shared.wndArea
    
    @State var searchEnter: Bool = false
    var curWebVisible: Bool { webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb }

    public init() {
        print("")
    }
    
    public var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    Color.black
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView(webCount: webcacheStore.cacheCount, isWebVisible: curWebVisible, webMonitor: webcacheStore.webWrappers[selectedTab.curIndex])
                            .frame(height: addressBar.isFocused ? 0 : dragScale.toolbarHeight)
                            .background(Color.bkColor)
                    }
                    
                    .background(Color.bkColor)
                    .environmentObject(webcacheStore)
                    .environmentObject(openingLink)
                    .environmentObject(selectedTab)
                    .environmentObject(addressBar)
                    .environmentObject(toolBarState)
                    .environmentObject(dragScale)
                    .environmentObject(wndArea)
                }
                .onAppear {
                    dragScale.onWidth = (geometry.size.width - 10) / screen_width
                }
                .onChange(of: geometry.size) { _, newSize in
                    dragScale.onWidth = (newSize.width - 10) / screen_width
                }
                
                .resizableSheet(isPresented: $toolBarState.showMoreMenu) {
                    SheetSegmentView(isShowingWeb: showWeb())
                        .environmentObject(selectedTab)
                        .environmentObject(openingLink)
                        .environmentObject(webcacheStore)
                        .environmentObject(dragScale)
                        .environmentObject(toolBarState)
                }
                .onChange(of: geometry.frame(in: .global)) { _, frame in
                    wndArea.frame = frame
                }
            }
            .environment(\.colorScheme, store.colorScheme)
            .task {
                if !searchEnter {
                    doSearchIfNeed()
                }
                
                if let key = BrowserViewStateStore.shared.searchKey, !key.isEmpty {
                    searchEnter = true
                    BrowserViewStateStore.shared.searchKey = nil
                }
            }
            .onChange(of: store.searchKey) { _, newValue in
                if !searchEnter {
                    doSearchIfNeed(key: newValue)
                }
                searchEnter.toggle()
            }
        }
        .clipped()
    }
    
    private func doSearchIfNeed(key: String? = BrowserViewStateStore.shared.searchKey) {
        guard let key = key, !key.isEmpty else {
            addressBar.isFocused = false
            return
        }
        if key.isURL() {
            BrowserViewStateStore.shared.searchKey = nil
            addressBar.searchInputText = key
            addressBar.isFocused = false
            let url = URL.createUrl(key)
            openingLink.clickedLink = url
        } else {
            enterType = .search
            addressBar.isFocused = true
            addressBar.searchInputText = key
        }
    }

    func showWeb() -> Bool {
        if webcacheStore.caches.count == 0 {
            return true
        }
        return webcacheStore.cache(at: BrowserViewStateStore.shared.selectedTab.curIndex).shouldShowWeb
    }
    
    
}