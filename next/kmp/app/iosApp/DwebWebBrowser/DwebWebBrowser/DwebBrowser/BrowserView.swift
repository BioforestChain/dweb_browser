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
    @State private var presentSheet = false
    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView(webMonitor: states.webcacheStore.webWrappers[states.selectedTabIndex].webMonitor)
                            .frame(height: states.addressBar.isFocused ? 0 : states.dragScale.toolbarHeight)
                    }
                    .background(.bk)
                    .environmentObject(states.webcacheStore)
                    .environmentObject(states.openingLink)
                    .environmentObject(states.addressBar)
                    .environmentObject(states.toolBarState)
                    .environmentObject(states.dragScale)
                    .environmentObject(states)
                }
                .resizableSheet(isPresented: $presentSheet) {
                    SheetSegmentView()
                        .environmentObject(states)
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
        states.doBackIfCan()
    }
    
    func resetStates(){
        states.clear()
    }
}
