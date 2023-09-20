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

    var body: some View {
        ZStack { GeometryReader { geometry in

            VStack(spacing: 0) {
                TabsContainerView()
                ToolbarView()
                    .frame()
                    .frame(minHeight: toolBarMinHeight, maxHeight: geometry.size.height * toolBarScale)
                    .background(Color.green)
            }
            .background(Color.bkColor)
            .environmentObject(webcacheStore)
            .environmentObject(openingLink)
            .environmentObject(selectedTab)
            .environmentObject(addressBar)
            .environmentObject(toolBarState)
        }
        }
    }
}
