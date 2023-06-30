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
    @EnvironmentObject var toolBar: ToolBarState
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
                VStack(spacing: 0){
                    ZStack{
                        VStack{
                            TabsContainerView()
                        }
                        KeyBoardShowingView(isFocused: $addressBar.isFocused)
                    }
                    AddressBarHStack()
                    ToolbarView()
                }
                .background(Color.bkColor)
                .environmentObject(openingLink)
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
                .environmentObject(showSheet)
                .sheet(isPresented: $showSheet.should){
                    SheetSegmentView()
                        .padding(.top, 28)
                        .environmentObject(selectedTab)
                        .environmentObject(openingLink)
                        .environmentObject(showSheet)
                        .presentationDetents([.medium, .large])
                }
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
