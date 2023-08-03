//
//  ScrollWholeView.swift
//  DwebBrowser
//
//  Created by ui06 on 7/3/23.
//

import SwiftUI

var disabledDragGesture = DragGesture().onChanged { _ in }.onEnded { _ in }

struct PagingScrollView: View {
    @ObservedObject var cacheMgr = WebCacheMgr.shared
    @EnvironmentObject var toolBarState: ToolBarState
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var animation: ShiftAnimation

    @StateObject var keyboard = KeyBoard()
    @Binding var showTabPage: Bool

    @State private var addressbarOffset: CGFloat = addressBarH

    var body: some View {
        GeometryReader { geometry in
            VStack {
                TabView(selection: $selectedTab.curIndex) {
                    ForEach(0 ..< cacheMgr.store.count, id: \.self) { index in
                        LazyVStack(spacing: 0) {
                            ZStack {
                                if showTabPage {
                                    ZStack {
                                        HStack {
                                            TabPageView(index: index, webWrapper: WebWrapperMgr.shared.store[index])
                                                .frame(height: geometry.size.height - addressBarH) // 使用GeometryReader获取父容器高度
                                                .gesture(disabledDragGesture)
                                        }

                                        if addressBar.isFocused {
                                            SearchTypingView()
                                        }
                                    }
                                }
                                else {
                                    Rectangle().fill(Color.clear)
                                        .frame(height: geometry.size.height - addressBarH)
                                        .gesture(disabledDragGesture)
                                }
                            }

                            AddressBar(index: index, webWrapper: WebWrapperMgr.shared.store[index], webCache: WebCacheMgr.cache(at: index))
                                .environmentObject(keyboard)
                                .frame(height: addressBarH)
                                .background(Color.bkColor)
                                .offset(y: addressbarOffset)
                                .animation(.default, value: addressbarOffset)
                                .gesture(addressBar.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                                .onChange(of: addressBar.shouldDisplay) { dispaly in
                                    addressbarOffset = dispaly ? 0 : addressBarH
                                }.onChange(of: addressBar.isFocused) { isFocused in
                                    addressbarOffset = isFocused ? -keyboard.height : 0
                                }
                        }
                        .frame(width: screen_width)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
            .onChange(of: animation.progress) { progress in
                if progress.isAnimating() {
                    showTabPage = false
                }
            }
        }
    }
}
