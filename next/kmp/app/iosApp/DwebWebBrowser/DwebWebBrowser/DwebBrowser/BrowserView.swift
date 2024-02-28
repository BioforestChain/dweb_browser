//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

import Combine

struct BrowserView: View {
    @ObservedObject var states = BrowserViewStates.shared
    @State var selectedTab = SelectedTab()
    @State var webcacheStore = WebCacheStore()
    @State var dragScale = WndDragScale()
    @State var openingLink = OpeningLink()
    @State private var presentSheet = false

    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView(webMonitor: webcacheStore.webWrappers[selectedTab.index].webMonitor)
                    }
                    .background(.bk)
                    .environment(webcacheStore)
                    .environment(dragScale)
                    .environment(openingLink)
                    .environmentObject(states.addressBar)
                    .environmentObject(states.toolBarState)
                    .environment(selectedTab)
                }
                .resizableSheet(isPresented: $presentSheet) {
                    SheetSegmentView(webCache: webcacheStore.cache(at: selectedTab.index))
                        .environment(selectedTab)
                        .environment(dragScale)
                        .environment(openingLink)
                        .environmentObject(states.toolBarState)
                }
                .onChange(of: geometry.size, initial: true) { _, newSize in
                    dragScale.onWidth = (newSize.width - 6.0) / screen_width
                }

                .onReceive(states.toolBarState.$showMoreMenu) { showMenu in
                    if showMenu {
                        presentSheet = true
                    } else {
                        if presentSheet {
                            presentSheet = false
                        }
                    }
                }
                .onChange(of: presentSheet) { _, present in
                    if present == false {
                        states.toolBarState.showMoreMenu = false
                    }
                }
            }
            .task {
                states.doSearchIfNeed() {
                    self.openingLink.clickedLink = $0
                }
                if states.searchKey != "" {
                    states.searchKey = ""
                }
            }
            .onChange(of: states.searchKey) { _, newValue in
                if !newValue.isEmpty {
                    states.doSearchIfNeed(key: newValue) {
                        self.openingLink.clickedLink = $0
                    }
                }
            }
        }
        .clipped()
    }

    func doNewTabUrl(url: String, target: String) {
        switch target {
        case AppBrowserTarget._blank.rawValue:
            states.toolBarState.createTabTapped = true
        case AppBrowserTarget._self.rawValue:
            break;
        // TODO: 打开系统浏览器暂未实现
        case AppBrowserTarget._system.rawValue:
            if url.isURL(), let searchUrl = URL(string: url) {
                UIApplication.shared.open(searchUrl)
                return;
            }
            break;
        default:
            break
        }
        states.doSearch(url)
    }

    func doSearch(searchKey: String) {
        states.doSearch(searchKey)
    }

    func updateColorScheme(color: Int) {
        states.updateColorScheme(newScheme: color)
    }

    func gobackIfCanDo() -> Bool {
        var webCanGoBack = false
        if webcacheStore.cache(at: selectedTab.index).isWebVisible,
           webcacheStore.webWrappers[selectedTab.index].webView.canGoBack
        {
            webCanGoBack = true
            webcacheStore.webWrappers[selectedTab.index].webView.goBack()
        }
        return states.doBackIfCan(isWebCanGoBack: webCanGoBack)
    }

    func resetStates() {
        states.clear()
        selectedTab.index = 0
        webcacheStore.resetWrappers()
        dragScale = WndDragScale()
        openingLink = OpeningLink()
    }
}
