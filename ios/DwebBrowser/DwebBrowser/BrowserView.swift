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
    @StateObject var showSheet = ShowSheet()
    @StateObject var toolBarState = ToolBarState()
    @State private var isNetworkSegmentViewPresented = false

    @EnvironmentObject var network: NetworkManager
    var body: some View {
        ZStack {
            GeometryReader { _ in
                VStack(spacing: 0) {
                    TabsContainerView()
                    ToolbarView()
                }
                .background(Color.bkColor)
                .environmentObject(openingLink)
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
                .environmentObject(showSheet)
                .environmentObject(toolBarState)

                .sheet(isPresented: $showSheet.should) {
                    SheetSegmentView(selectedCategory: WebCacheMgr.shared.store[selectedTab.curIndex].shouldShowWeb ? .menu : .bookmark)
                        .environmentObject(selectedTab)
                        .environmentObject(openingLink)
                        .environmentObject(showSheet)
                        .presentationDetents([.medium, .large])
                }

                .sheet(isPresented: $isNetworkSegmentViewPresented) {
                    NetworkGuidView()
                }
                .onReceive(network.$isNetworkAvailable) { isAvailable in
                    isNetworkSegmentViewPresented = !isAvailable
                }
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView().environmentObject(NetworkManager())
    }
}
