//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct BrowserView: View {
    @StateObject var selectedTab = SelectedTab()
    @StateObject var addressBar = AddressBarState()
    @StateObject var openingLink = OpeningLink()
    @StateObject var toolBarState = ToolBarState()
    @StateObject var webcacheStore = WebCacheStore()
    @StateObject var dragScale = WndDragScale()
    @StateObject var wndArea = BrowserArea()

    @Binding var size: CGSize
    @State private var colorScheme = ColorScheme.light
    var body: some View {
        ZStack {
            GeometryReader { geometry in
                ZStack {
                    Color.black
                    VStack(spacing: 0) {
                        TabsContainerView()
                        ToolbarView()
                            .frame(height: dragScale.toolbarHeight)
                            .background(Color.bkColor)
                    }
                    
                    .background(Color.bkColor)
                    .environmentObject(webcacheStore)
                    .environmentObject(openingLink)
                    .environmentObject(selectedTab)
                    .environmentObject(addressBar)
                    .environmentObject(toolBarState)
                    .environmentObject(dragScale)
                    .environmentObject(wndArea)
                }
                .frame(width: size.width, height: size.height)
                
                .onAppear {
                    dragScale.onWidth = (geometry.size.width - 10) / screen_width
                }
                .onChange(of: size) { _, newSize in
                    dragScale.onWidth = (newSize.width - 10) / screen_width
                }
                
                .resizableSheet(isPresented: $toolBarState.showMoreMenu) {
                    SheetSegmentView(isShowingWeb: showWeb())
                        .environmentObject(selectedTab)
                        .environmentObject(openingLink)
                        .environmentObject(webcacheStore)
                        .environmentObject(dragScale)
                        .environmentObject(toolBarState)
                }
                .onChange(of: geometry.frame(in: .global)) { _, frame in
                    wndArea.frame = frame
//                    Log("window rect:(\(frame.origin)), (\(frame.size))")
                }
            }
            .environment(\.colorScheme, colorScheme)
            .onReceive(KmpBridgeManager.shared.eventPublisher.debounce(for: 0.3, scheduler: DispatchQueue.main).filter{$0.name == KmpEvent.colorScheme}) { e in
                Log("Color scheme")
                guard let scheme = e.inputDatas?["colorScheme"] as? String else {
                    return
                }
                if scheme == "dark" {
                    colorScheme = .dark
                } else {
                    colorScheme = .light
                }
            }
        }
    }

    func showWeb() -> Bool {
        if webcacheStore.caches.count == 0 {
            return true
        }
        return webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb
    }
}
