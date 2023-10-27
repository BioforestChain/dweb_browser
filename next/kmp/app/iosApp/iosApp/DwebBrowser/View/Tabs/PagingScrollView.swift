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
    

    @StateObject var keyboard = KeyBoard()
    @Binding var showTabPage: Bool

    @State private var addressbarOffset: CGFloat = 0
    var webwrappers: [WebWrapper] { webcacheStore.webWrappers }
    
    var body: some View {
        GeometryReader { geometry in
            VStack {
                TabView(selection: $selectedTab.curIndex) {
                    
                    ForEach(0 ..< webcacheStore.cacheCount, id: \.self) { index in
                        LazyVStack(spacing: 0) {
                            ZStack {
                                if showTabPage {
                                    ZStack {
                                        HStack {
                                            TabPageView(index: index, webWrapper: webwrappers[index])
                                                .frame(height: geometry.size.height - addressBarH) // 使用GeometryReader获取父容器高度
                                                .gesture(disabledDragGesture)
                                        }

                                        if addressBar.isFocused {
                                            SearchTypingView()
                                                .environmentObject(webcacheStore)
                                        }
                                    }
                                } else {
                                    Rectangle().fill(Color.clear)
                                        .frame(height: geometry.size.height - addressBarH)
                                        .gesture(disabledDragGesture)
                                }
                            }

                            AddressBar(index: index, webWrapper: webwrappers[index], webCache: webcacheStore.cache(at: index))
                                .environmentObject(keyboard)
                                .background(Color.bkColor)
                                .offset(y: addressbarOffset)
                                .animation(.default, value: addressbarOffset)
                                .gesture(addressBar.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                                .onChange(of: addressBar.shouldDisplay) { dispaly in
                                    addressbarOffset = dispaly ? 0 : addressBarH
                                }

                                .onChange(of: keyboard.height) { height in
                                    if #available(iOS 16.3, *) {
                                        printWithDate("observed keyboard height has changed")

#if !DwebBrowser
                                        printWithDate( "now we are in C#########")
                                        if index == selectedTab.curIndex {
                                            if height == 0 {
                                                addressbarOffset = 0
                                            } else {
                                                addressbarOffset = -height
                                            }
                                            printWithDate( "addressbarOffset is \(addressbarOffset)")
                                        }
#endif
                                    }
                                }
                        }
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
