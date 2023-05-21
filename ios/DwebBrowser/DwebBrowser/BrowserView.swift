//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

/*
 some views that need to be visited in outer view
some methods and some datas might be visited in somewhere of the whole app
 */
class SharedSourcesVM: ObservableObject {
    
}


struct BrowserView: View {
    @ObservedObject var browser = BrowerVM()

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
                .coordinateSpace(name: "Root")
                .environmentObject(ToolbarState())
                .environmentObject(browser)
                .background(Color(white: 0.7))
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
