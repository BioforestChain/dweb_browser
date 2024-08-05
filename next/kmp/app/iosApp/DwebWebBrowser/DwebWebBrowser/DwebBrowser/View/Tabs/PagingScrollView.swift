//
//  ScrollWholeView.swift
//  DwebBrowser
//
//  Created by ui06 on 7/3/23.
//

import SwiftUI

var disabledDragGesture = DragGesture().onChanged { _ in }.onEnded { _ in }

struct PagingScrollView: View {
    @Environment(SelectedTab.self) var seletecdTab
    @Environment(WebCacheStore.self) var webcacheStore
    @Environment(ToolBarState.self) var toolBarState
    @Environment(AddressBarState.self) var addressBar
    @Environment(ShiftAnimation.self) var animation
    @Environment(WndDragScale.self) var dragScale
    
    @State var menuAction: DragDownMenuAction = .none

    @Binding var showTabPage: Bool
    @State private var addressbarOffset: CGFloat = 0
    var body: some View {
        @Bindable var seletecdTab = seletecdTab
        GeometryReader { geometry in
            VStack {
                TabView(selection: $seletecdTab.index) {
                    ForEach(webcacheStore.caches.indices, id: \.self) { (index: Int) in
                        if index < webcacheStore.caches.count { //防止在视图重绘的时候，数组元素变化引起越界。
                            let cache = webcacheStore.cache(at: index)
                            let webwrapper = webcacheStore.webWrappers[index]
                            LazyVStack(spacing: 0) {
                                ZStack {
                                    if showTabPage {
                                        ZStack {
                                            HStack {
                                                TabPageView(webCache: cache, webWrapper: webwrapper,
                                                            isVisible: index == seletecdTab.index,
                                                            doneLoading: loadingFinished,
                                                            menuAction: $menuAction)
                                                .highPriorityGesture(disabledDragGesture)
                                                .onChange(of: menuAction) { _, newValue in
                                                    menuActionChanged(action: newValue, cacheId: cache.id, index: index)
                                                }
                                            }
                                            if addressBar.isFocused {
                                                SearchTypingView()
                                            }
                                        }
                                    } else {
                                        Rectangle().fill(Color.clear)
                                            .highPriorityGesture(disabledDragGesture)
                                    }
                                }
                                .frame(height: max(geometry.size.height - dragScale.addressbarHeight, 0))
                                AddressBar(webCache: cache,
                                           tabIndex: index,
                                           isVisible: index == seletecdTab.index)
                                .environment(webcacheStore.webWrappers[index].webMonitor)
                                .background(.bk)
                                .offset(y: addressbarOffset)
                                .animation(.default, value: addressbarOffset)
                                .highPriorityGesture(addressBar.isFocused ? disabledDragGesture : nil)
                                .onChange(of: addressBar.shouldDisplay) { _, dispaly in
                                    addressbarOffset = dispaly ? 0 : dragScale.addressbarHeight
                                }
                            }
                        }
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("PagingScrollView")
    }
    
    
}

extension PagingScrollView {
    private func loadingFinished(webCache: WebCache) {
        webcacheStore.saveCaches()
        if !TracelessMode.shared.isON {
            DwebBrowserHistoryStore.shared.addHistoryRecord(title: webCache.title, url: webCache.lastVisitedUrl.absoluteString)
        }
    }
    
    private func menuActionChanged(action: DragDownMenuAction, cacheId: UUID, index: Int){
        if action != .none {
            if action == .closeTab{
                webcacheStore.remove(by: cacheId)
            } else if action == .refreshTab{
                addressBar.needRefreshOfIndex = index
            } else if action == .createNewTab {
                withAnimation {
                    toolBarState.shouldCreateTab = true
                } completion: {
                    seletecdTab.index = webcacheStore.cacheCount - 1
                }
            }
            menuAction = .none
        }
    }
}
