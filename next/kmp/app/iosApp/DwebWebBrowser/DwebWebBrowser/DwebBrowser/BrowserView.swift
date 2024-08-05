//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

import Combine

struct BrowserView: View {
    @State var addressBar = AddressBarState()
    @State var selectedTab = SelectedTab()
    @State var webcacheStore = WebCacheStore()
    @State var dragScale = WndDragScale()
    @State var openingLink = OpeningLink()
    @State var toolbarState = ToolBarState()
    @State var outerSearch = OuterSearch()
    @State var pullingMenu = PullingMenu()
    let sheetState = ResizeSheetState()

    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView()
                            .environment(webcacheStore.webWrappers[selectedTab.index].webMonitor)
                    }
                    .background(.bk)
                    .environment(webcacheStore)
                    .environment(dragScale)
                    .environment(openingLink)
                    .environment(addressBar)
                    .environment(toolbarState)
                    .environment(selectedTab)
                    .environment(outerSearch)
                    .environment(pullingMenu)
                }
                .resizableSheet(isPresented: sheetState) {
                    SheetSegmentView(webCache: webcacheStore.cache(at: selectedTab.index))
                        .environment(selectedTab)
                        .environment(dragScale)
                        .environment(openingLink)
                        .environment(toolbarState)
                        .environment(sheetState)
                }
                .onChange(of: geometry.size, initial: true) { _, newSize in
                    dragScale.onWidth = (newSize.width - 6.0) / screen_width
                }
                .onChange(of: toolbarState.showMoreMenu) { _, showMenu in
                    if showMenu {
                        sheetState.presenting = true
                    } else {
                        if sheetState.presenting {
                            sheetState.presenting = false
                        }
                    }
                }
                .onChange(of: sheetState.presenting) { _, present in
                    if present == false {
                        toolbarState.showMoreMenu = false
                    }
                }
            }
        }
        .clipped()
    }

    func doNewTabUrl(url: String, target: String) {
        switch target {
        case AppBrowserTarget._blank.rawValue:
            toolbarState.newTabUrl = url.isURL() ? URL(string: url)! : emptyURL
            toolbarState.shouldCreateTab = true
        case AppBrowserTarget._self.rawValue:
            break
        // TODO: 打开系统浏览器暂未实现
        case AppBrowserTarget._system.rawValue:
            if url.isURL(), let searchUrl = URL(string: url) {
                UIApplication.shared.open(searchUrl)
                return
            }
        default:
            break
        }
        searchFromOutside(outerSearchKey: url)
    }

    func searchFromOutside(outerSearchKey: String) {
        if outerSearchKey != "", outerSearch.content != outerSearchKey {
            tryOuterTextSearch(searchText: outerSearchKey)
        }
    }

    func updateColorScheme(color: Int) {
        ColorSchemeManager.shared.colorScheme = LocalColorScheme(rawValue: color)!
    }

    func loadPullMenuConfig(isActived: Bool) {
        pullingMenu.isActived = isActived
    }

    func gobackIfCanDo() -> Bool {
        if addressBar.isFocused {
            return true
        }

        if toolbarState.showMoreMenu {
            toolbarState.showMoreMenu = false
            return true
        }

        if toolbarState.tabsState == .shrinked {
            toolbarState.tabsState = .shouldExpand
            return true
        }

        if toolbarState.isPresentingScanner {
            toolbarState.isPresentingScanner = false
            return true
        }

        var webCanGoBack = false
        if webcacheStore.cache(at: selectedTab.index).isWebVisible,
           webcacheStore.webWrappers[selectedTab.index].webView.canGoBack
        {
            webCanGoBack = true
            webcacheStore.webWrappers[selectedTab.index].webView.goBack()
        }
        return webCanGoBack
    }

    private func tryOuterTextSearch(searchText: String) {
        guard searchText != "" else {
            addressBar.isFocused = false
            return
        }

        var deadline: CGFloat = 0.0
        if toolbarState.tabsState == .shrinked {
            deadline = 0.5
            toolbarState.tabsState = .shouldExpand
        }

        if toolbarState.showMoreMenu {
            deadline = 0.5
            toolbarState.showMoreMenu = false
        }

        if searchText.isURL() {
            addressBar.isFocused = false
            let url = URL.createUrl(searchText)

            DispatchQueue.main.asyncAfter(deadline: .now() + deadline) {
                openingLink.clickedLink = url
            }
        } else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { // 如果从外部搜索启动browser，需要等某些view初始化完成
                outerSearch.shouldDoSearch = true
                outerSearch.content = searchText
            }
        }
    }

    func resetStates() {
        selectedTab.index = 0
        webcacheStore.resetWrappers()
        dragScale = WndDragScale()
        openingLink = OpeningLink()
        toolbarState = ToolBarState()
        outerSearch = OuterSearch()
        addressBar = AddressBarState()
    }

    func destroy() {
        webcacheStore.webWrappers = []
        webcacheStore.caches = []
    }
}
