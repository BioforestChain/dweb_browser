//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct BrowserView: View {
    @ObservedObject var states: BrowserViewStates
    @ObservedObject var toolBarState: ToolBarState
    var curWebVisible: Bool { states.webcacheStore.cache(at: states.selectedTab.curIndex).shouldShowWeb }

    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView(webCount: states.webcacheStore.cacheCount,
                                    isWebVisible: curWebVisible,
                                    webMonitor: states.webcacheStore.webWrappers[states.selectedTab.curIndex])
                            .frame(height: states.addressBar.isFocused ? 0 : states.dragScale.toolbarHeight)
                    }
                    .background(Color.bkColor)
                    .environmentObject(states.webcacheStore)
                    .environmentObject(states.openingLink)
                    .environmentObject(states.selectedTab)
                    .environmentObject(states.addressBar)
                    .environmentObject(states.toolBarState)
                    .environmentObject(states.dragScale)
                    .environmentObject(states.wndArea)
                }
                .onAppear {
                    states.dragScale.onWidth = (geometry.size.width - 10) / screen_width
                }
                .onChange(of: geometry.size) { _, newSize in
                    states.dragScale.onWidth = (newSize.width - 10) / screen_width
                }

                .resizableSheet(isPresented: $toolBarState.showMoreMenu) {
                    SheetSegmentView(isShowingWeb: showWeb())
                        .environmentObject(states.selectedTab)
                        .environmentObject(states.openingLink)
                        .environmentObject(states.webcacheStore)
                        .environmentObject(states.dragScale)
                        .environmentObject(states.toolBarState)
                }
                .onChange(of: geometry.frame(in: .global)) { _, frame in
                    states.wndArea.frame = frame
                }
            }
            .task {
                states.doSearchIfNeed()
                if let key = states.searchKey, !key.isEmpty {
                    states.searchKey = nil
                }
            }
            .onChange(of: states.searchKey) { _, newValue in
                if let newValue = newValue, !newValue.isEmpty {
                    states.doSearchIfNeed(key: newValue)
                }
            }
        }
        .clipped()
    }

    func showWeb() -> Bool {
        if states.webcacheStore.caches.count == 0 {
            return true
        }
        return states.webcacheStore.cache(at: states.selectedTab.curIndex).shouldShowWeb
    }
}
