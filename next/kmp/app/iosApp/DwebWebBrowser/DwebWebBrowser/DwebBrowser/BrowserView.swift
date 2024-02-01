//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit
import Combine

struct BrowserView: View {
    @ObservedObject private var states = BrowserViewStates.shared
    @State private var selectedTab = SelectedTab()
    @State private var presentSheet = false
    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView(webMonitor: states.webcacheStore.webWrappers[selectedTab.index].webMonitor)
                    }
                    .background(.bk)
                    .environment(states.webcacheStore)
                    .environmentObject(states.openingLink)
                    .environmentObject(states.addressBar)
                    .environmentObject(states.toolBarState)
                    .environmentObject(states.dragScale)
                    .environment(selectedTab)
                }
                .resizableSheet(isPresented: $presentSheet) {
                    SheetSegmentView(webCache: states.webcacheStore.cache(at: selectedTab.index))
                        .environment(selectedTab)
                        .environmentObject(states.openingLink)
                        .environmentObject(states.dragScale)
                        .environmentObject(states.toolBarState)
                }
                .onChange(of: geometry.size, initial: true) { _, newSize in
                    states.dragScale.onWidth = (newSize.width - 6.0) / screen_width
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
    
    func doSearch(searchKey: String){
        states.doSearch(searchKey)
    }
    func updateColorScheme(color: Int){
        states.updateColorScheme(newScheme: color)
    }
    func gobackIfCanDo() -> Bool{
        states.doBackIfCan(selected: selectedTab.index)
    }
    
    func resetStates(){
        states.clear()
        selectedTab.index = 0
    }
}
