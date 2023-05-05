//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

class MainViewState: ObservableObject{
    @Published var adressBarHstackOffset: CGFloat = 0.0
    @Published var currentTabIndex: Int = 0
    @Published var addressBarHeight: CGFloat = 60
}



class WebViewState: ObservableObject {
    @Published var loadingProgress: CGFloat = 0
    @Published var canGoForward: Bool = false
    @Published var canGoback: Bool = false
}

struct MainContainerView: View {
    @StateObject var tabPageStates = TabPageStates()
    var body: some View {
        ZStack{
            GeometryReader{ sGgeometry in
                
                VStack(spacing: 0){
                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合
                    TabsContainerView()
                    Divider().background(Color(.darkGray))
                    AddressBarHStack()
                    ToolbarView()
                }
                .frame(height: sGgeometry.size.height)
                .coordinateSpace(name: "Root")
                .environmentObject(ToolbarState())
                .environmentObject(MainViewState())
                .environmentObject(tabPageStates)
                .environmentObject(WebPages())
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        MainContainerView()
    }
}
