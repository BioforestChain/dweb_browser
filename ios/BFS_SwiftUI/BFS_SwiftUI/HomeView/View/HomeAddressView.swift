//
//  HomeAddressView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/27.
//

import SwiftUI

struct HomeAddressView: View {
    
    @EnvironmentObject var webViewModel: WebViewViewModel
    @State var currentIndex: Int = 0
    @State private var addressBarList: [PageModel] = [PageModel(index: 0),PageModel(index: 1),PageModel(index: 2)]
    
    var body: some View {
        
        CarouselView(spacing: 10, trailingSpace: 50,index: $currentIndex, items: addressBarList) { _ in
            
            GeometryReader { proxy in
                
                BrowserAddressBarView(webViewModel: webViewModel)
                    .frame(width: proxy.size.width)
//                    .padding(EdgeInsets(top: 1, leading: 24, bottom: 0, trailing: 24))
                    .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
            }
        }
        .frame(height: 45)
        
        
        
        
//        BrowserAddressBarView(webViewModel: webViewModel)
//            .padding(EdgeInsets(top: 1, leading: 24, bottom: 0, trailing: 24))
//            .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
            
    }
}

struct HomeAddressView_Previews: PreviewProvider {
    static var previews: some View {
        HomeAddressView()
    }
}

struct PageModel: Identifiable {
    
    var id = UUID().uuidString
    var index: Int
}
