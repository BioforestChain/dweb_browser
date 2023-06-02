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
    @State var wrapperCount:Int = 1
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
                VStack(spacing: 0){
                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                    TabsContainerView()
                    Divider().background(Color(.darkGray))
                    AddressBarHStack(selectedTabIndex: $browser.selectedTabIndex)
                    ToolbarView(selectedTabIndex: $browser.selectedTabIndex)
                }
                .coordinateSpace(name: "Root")
                .environmentObject(browser)
//                .background(Color(white: 0.7))
            }
        }
    }

}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
