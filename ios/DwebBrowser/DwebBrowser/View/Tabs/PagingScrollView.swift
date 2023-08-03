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
//    @StateObject var keyboardHelper = KeyboardHeightHelper()

    
    @State private var keyboardHeight = 0.0
    @Binding var showTabPage: Bool

    @State private var addressbarOffset: CGFloat = addressBarH

    var body: some View {
        GeometryReader { geometry in
            VStack {
                TabView(selection: $selectedTab.curIndex) {
                    ForEach(0 ..< cacheMgr.store.count, id: \.self) { index in
                        LazyVStack(spacing: 0) {
                            ZStack{
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
                                else{
                                    Rectangle().fill(Color.clear)
                                        .frame(height: geometry.size.height - addressBarH)
                                        .gesture(disabledDragGesture)
                                }
                            }
                            
                            AddressBar(index: index, webWrapper: WebWrapperMgr.shared.store[index], webCache: WebCacheMgr.cache(at: index))
                                .frame(height: addressBarH)
                                .background(Color.bkColor)
                                .offset(y: updateKeyboardOffset())
                                .animation(.default, value: updateKeyboardOffsetAnimation())
                                .gesture(addressBar.isFocused ? disabledDragGesture : nil) // 根据状态变量决定是否启用拖拽手势
                                .onChange(of: addressBar.shouldDisplay) { dispaly in
                                    addressbarOffset = dispaly ? 0 : addressBarH
                                }.onChange(of: addressBar.isFocused) { isFocused in
                                    addressbarOffset = isFocused ? -keyboardHeight : 0
                                }
                                .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { notify in
                                    // 当视图获得焦点时
                                    guard let value = notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
                                    let height = value.height
                                    keyboardHeight = height - safeAreaBottomHeight
                                }
                                .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
                                    // 当视图获得焦点时
                                    keyboardHeight = 0
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
    
    func updateKeyboardOffset() -> CGFloat {
        #if DwebFramework
            if addressBar.isFocused {
                return -keyboardHeight
            } else {
                return addressbarOffset
            }
        #endif
        
        return addressbarOffset
    }
    
    func updateKeyboardOffsetAnimation() -> CGFloat {
        #if DwebFramework
            return keyboardHeight
        #endif
        
        return addressbarOffset
    }
}
