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
    @EnvironmentObject var toolBarState: ToolBarState
    @EnvironmentObject var addressBar: AddressBarState
    @Environment(ShiftAnimation.self) var animation
    @Environment(WndDragScale.self) var dragScale

    @Binding var showTabPage: Bool
    @State private var addressbarOffset: CGFloat = 0
    var body: some View {
        @Bindable var seletecdTab = seletecdTab
        GeometryReader { geometry in
            VStack {
                TabView(selection: $seletecdTab.index) {
                    ForEach(webcacheStore.caches.indices, id: \.self) { (index: Int) in
                        let cache = webcacheStore.cache(at: index)
                        let webwrapper = webcacheStore.webWrappers[index]
                        LazyVStack(spacing: 0) {
                            ZStack {
                                if showTabPage {
                                    ZStack {
                                        HStack {
                                            TabPageView(webCache: cache, webWrapper: webwrapper,
                                                        isVisible: index == seletecdTab.index,
                                                        doneLoading: loadingFinished)
                                                .highPriorityGesture(disabledDragGesture)
                                        }
                                        if addressBar.isFocused{
                                            SearchTypingView()
                                        }
                                    }
                                } else {
                                    Rectangle().fill(Color.clear)
                                        .highPriorityGesture(disabledDragGesture)
                                }
                            }
                            .frame(height: max(geometry.size.height - dragScale.addressbarHeight, 0))
                            AddressBar(webMonitor: webcacheStore.webWrappers[index].webMonitor, 
                                       webCache: cache,
                                       tabIndex: index,
                                       isVisible: index == seletecdTab.index)

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
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
            
        }
    }

    func loadingFinished(webCache: WebCache) {
        webcacheStore.saveCaches()
        if !TracelessMode.shared.isON {
            DwebBrowserHistoryStore.shared.addHistoryRecord(title: webCache.title, url: webCache.lastVisitedUrl.absoluteString)
        }
    }
}
