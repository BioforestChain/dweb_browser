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
                    .environmentObject(states.openingLink)
                    .environmentObject(states.addressBar)
                    .environmentObject(states.toolBarState)
                    .environment(selectedTab)
                }
                .resizableSheet(isPresented: $presentSheet) {
                    SheetSegmentView(webCache: webcacheStore.cache(at: selectedTab.index))
                        .environment(selectedTab)
                        .environment(dragScale)
                        .environmentObject(states.openingLink)
                        .environmentObject(states.toolBarState)
                }
                .onChange(of: geometry.size, initial: true) { _, newSize in
                    dragScale.onWidth = (newSize.width - 6.0) / screen_width
                }

                .onReceive(states.toolBarState.$showMoreMenu) { showMenu in
                    if showMenu {
                        presentSheet = true
                    }else{
                        if presentSheet{
                            presentSheet = false
                        }
                    }
                }
                .onChange(of: presentSheet) { oldValue, present in
                    if present == false{
                        states.toolBarState.showMoreMenu = false
                    }
                }
            }
            .task {
                states.doSearchIfNeed()
                if let key = states.searchKey, !key.isEmpty {
                    states.searchKey = nil
                }
            }
            .onChange(of: states.searchKey) { _, newValue in
                if let newValue = newValue, !newValue.isEmpty {
                    states.doSearchIfNeed(key: newValue)
                }
            }
        }
        .clipped()
    }
    
    func doNewTabUrl(url: String, blank: Bool) {
        if blank {
            webcacheStore.createOne()
        }
        states.doSearch(url)
    }
    
    func doSearch(searchKey: String){
        states.doSearch(searchKey)
    }
    func updateColorScheme(color: Int){
        states.updateColorScheme(newScheme: color)
    }
    
    func gobackIfCanDo() -> Bool {
        var webCanGoBack = false
        if webcacheStore.cache(at: selectedTab.index).isWebVisible,
           webcacheStore.webWrappers[selectedTab.index].webView.canGoBack{
            webCanGoBack = true
            webcacheStore.webWrappers[selectedTab.index].webView.goBack()
        }
        return states.doBackIfCan(isWebCanGoBack: webCanGoBack)
    }
    
    func resetStates(){
        states.clear()
        selectedTab.index = 0
        webcacheStore.resetWrappers()
        dragScale = WndDragScale()
    }
}
