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
    @EnvironmentObject var toolBar: ToolBarState
    @State private var shouldShowSheet = false
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
                VStack(spacing: 0){
                    ZStack{
                        VStack{
                            Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                            TabsContainerView()
//                            Divider().background(Color(.darkGray))
                        }
                        KeyBoardShowingView(isFocused: $addressBar.isFocused)
                    }
                    AddressBarHStack()
                    ToolbarView(shouldShowSheet: $shouldShowSheet)
                }
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
                .background(Color.bkColor)
                
                .halfSheet(showSheet: $shouldShowSheet) {
                    ZStack { 
                        Color.white
                        HalfSheetPickerView()
                            .environmentObject(selectedTab)
                    }
                    .padding(.top, 28)
                    .background(.white)
                    .cornerRadius(10)
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
