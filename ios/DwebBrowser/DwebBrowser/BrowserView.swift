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
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
//                VStack(spacing: 0){
//                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
//                    TabsContainerView()
//                    Divider().background(Color(.darkGray))
//                    AddressBarHStack()
//                    ToolbarView()
//                }
//                .coordinateSpace(name: "Root")
//                .environmentObject(selectedTab)
                VStack{
                    ZStack{
                        VStack{
                            Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                            TabsContainerView()
                            Divider().background(Color(.darkGray))
                        }
                        OverlayMaskView(isFocused: $addressBar.isFocused)
                    }
                    AddressBarHStack()
                    ToolbarView()
                }
//                .coordinateSpace(name: "Root")
                .environmentObject(selectedTab)
                .environmentObject(addressBar)
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
