//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct BrowserView: View {
    @ObservedObject var browser = BrowerVM()
    @ObservedObject var addressBar = AddressBarState()
    @State var wrapperCount:Int = 1
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
                VStack(spacing: 0){
                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                    TabsContainerView()
                    Divider().background(Color(.darkGray))
                    AddressBarHStack()
                    ToolbarView(selectedTabIndex: $browser.selectedTabIndex)
                }
                .coordinateSpace(name: "Root")
                .environmentObject(browser)
//                VStack{
//                    ZStack{
//                        VStack{
//                            Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
//                            TabsContainerView()
//                            Divider().background(Color(.darkGray))
//                        }
//                        OverlayMaskView(isEditing: Binding(get: { addressBar.isFocused }, set: { addressBar.isFocused = $0 }))
//                    }
//                    AddressBarHStack()
//                    ToolbarView(selectedTabIndex: $browser.selectedTabIndex)
//                }
//                .coordinateSpace(name: "Root")
//                .environmentObject(browser)
//                .environmentObject(addressBar)
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
