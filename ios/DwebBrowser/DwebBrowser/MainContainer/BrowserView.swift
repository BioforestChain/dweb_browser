//
//  MainContainerView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

class WebViewState: ObservableObject {
    @Published var loadingProgress: CGFloat = 0
    @Published var canGoForward: Bool = false
    @Published var canGoback: Bool = false
}

/*
 some views that need to be visited in outer view
some methods and some datas might be visited in somewhere of the whole app
 */
class SharedSourcesVM: ObservableObject {
    @Published var webViews: [WebView] = []
    func shiftPage(image: UIImage){
        
    }
}


struct BrowserView: View {
    @ObservedObject var browser = BrowerVM()
    @ObservedObject var sharedResources = SharedSourcesVM()

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
//                .environmentObject(browser)
                
            }
        }
    }
}

struct MainContainerView_Previews: PreviewProvider {
    static var previews: some View {
        BrowserView()
    }
}
