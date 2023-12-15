//
//  ScrollWholeView.swift
//  DwebBrowser
//
//  Created by ui06 on 7/3/23.
//

import SwiftUI

var disabledDragGesture = DragGesture().onChanged { _ in }.onEnded { _ in }

struct PagingScrollView: View {
    @EnvironmentObject var toolBarState: ToolBarState
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var animation: ShiftAnimation
    @EnvironmentObject var webcacheStore: WebCacheStore
    @EnvironmentObject var dragScale: WndDragScale

    @Binding var showTabPage: Bool
    @State private var addressbarOffset: CGFloat = 0
    var body: some View {
        GeometryReader { geometry in
            VStack {
                TabView(selection: $selectedTab.curIndex) {
                    ForEach(webcacheStore.caches.indices, id: \.self) { (index: Int) in
                        let cache = webcacheStore.cache(at: index)
                        let webwrapper = webcacheStore.webWrappers[index]
                        LazyVStack(spacing: 0) {
                            ZStack {
                                if showTabPage {
                                    ZStack {
                                        HStack {
                                            TabPageView(webCache: cache, webWrapper: webwrapper)
                                                .id(UUID())
                                                .gesture(disabledDragGesture)
                                        }
                                        if addressBar.isFocused {
                                            SearchTypingView()
                                                .environmentObject(webcacheStore)
                                        }
                                    }
                                } else {
                                    Rectangle().fill(Color.clear)
                                        .gesture(disabledDragGesture)
                                }
                            }
                            .frame(height: geometry.size.height - dragScale.addressbarHeight)

                            AddressBar(webCache: cache,
                                       webMonitor: webcacheStore.webWrappers[index].webMonitor,
                                       tabIndex: index,
                                       isVisible: index == selectedTab.curIndex)

                                .background(Color.bkColor)
                                .offset(y: addressbarOffset)
                                .animation(.default, value: addressbarOffset)
                                .gesture(addressBar.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                                .onChange(of: addressBar.shouldDisplay) { _, dispaly in
                                    addressbarOffset = dispaly ? 0 : dragScale.addressbarHeight
                                }
                        }
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
        }
    }
}
