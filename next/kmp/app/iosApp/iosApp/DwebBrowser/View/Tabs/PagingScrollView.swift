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
                    ForEach(0 ..< webcacheStore.cacheCount, id: \.self) { index in
                        let cache = webcacheStore.cache(at: index)
                        let webwrapper = webcacheStore.webWrappers[index]
                        LazyVStack(spacing: 0) {
                            ZStack {
                                if showTabPage {
                                    ZStack {
                                        HStack {
                                            TabPageView(webCache: cache, webWrapper: webwrapper)
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

                            AddressBar(webWrapper: webwrapper, webCache: cache)
                                .background(Color.bkColor)
                                .offset(y: addressbarOffset)
                                .animation(.default, value: addressbarOffset)
                                .gesture(addressBar.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                                .onChange(of: addressBar.shouldDisplay) { dispaly in
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


struct OffsetViewView: View {
    @State private var scrollOffset: CGFloat = 0

    var body: some View {
        ScrollView {
            ScrollViewReader { proxy in
                VStack {
                    ForEach(0..<50) { index in
                        Text("Item \(index)")
                            .frame(height: 50)
                    }
                }
                .onAppear {
                    // Scroll to a specific view when the view appears
                    proxy.scrollTo(25, anchor: .top)
                }
                .background(
                    GeometryReader { geometry in
                        Color.clear.onAppear {
                            // Update the scrollOffset based on geometry information
                            scrollOffset = geometry.frame(in: .named("scroll")).minY
                        }
                    }
                )
            }
        }
        .coordinateSpace(name: "scroll")
        .overlay(
            Text("Scroll Offset: \(scrollOffset)")
                .padding()
                .background(Color.yellow)
                .cornerRadius(10)
                .opacity(0.8)
                .padding()
                .frame(width:100)
        )
    }
}

#Preview{
    OffsetViewView()
}
