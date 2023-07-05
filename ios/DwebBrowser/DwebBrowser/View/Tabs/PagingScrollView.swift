//
//  ScrollWholeView.swift
//  DwebBrowser
//
//  Created by ui06 on 7/3/23.
//

import SwiftUI

struct PagingScrollView: View {
    @ObservedObject var cacheMgr = WebCacheMgr.shared
    @EnvironmentObject var toolBarState: ToolBarState
    @EnvironmentObject var addressBarState: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    private var disabledDragGesture = DragGesture().onChanged { _ in }.onEnded { _ in }

    var body: some View {
        VStack {
            TabView(selection: $selectedTab.curIndex) {
                ForEach((0 ..< cacheMgr.store.count), id: \.self) { index in
                    GeometryReader { geometry in
                        LazyVStack(spacing: 0) {
                            ZStack{
                                HStack {
                                    TabPageView(index: index)
                                        .frame(height: geometry.size.height - toolBarState.addressBarHeight) // 使用GeometryReader获取父容器高度
                                        .gesture(disabledDragGesture)
                                }
                                if addressBarState.isFocused {
                                    SearchTypingView()
                                }
                            }
                            AddressBar(index: index)
                                .frame(height: toolBarState.addressBarHeight)
                                .gesture(addressBarState.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                        }
                        .frame(width: screen_width)
                    }
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
        }
    }
}

