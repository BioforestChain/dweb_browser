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
    @ObservedObject var keyboardHelper = KeyboardHeightHelper()
    private var disabledDragGesture = DragGesture().onChanged { _ in }.onEnded { _ in }

    var body: some View {
        GeometryReader { geometry in
            VStack {
                TabView(selection: $selectedTab.curIndex) {
                    ForEach(0 ..< cacheMgr.store.count, id: \.self) { index in
                        LazyVStack(spacing: 0) {
                            ZStack {
                                HStack {
                                    TabPageView(index: index)
                                        .frame(height: geometry.size.height - addressBarH) // 使用GeometryReader获取父容器高度
                                        .gesture(disabledDragGesture)
                                }
                                if addressBarState.isFocused {
                                    SearchTypingView()
                                }
                            }
                            AddressBar(index: index)
                                .offset(y: addressBarState.isFocused ? -keyboardHelper.keyboardHeight : 0)
                                .animation(.spring(), value: keyboardHelper.keyboardHeight)
                                .gesture(addressBarState.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                        }
                        .frame(width: screen_width)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
        }
    }
}
