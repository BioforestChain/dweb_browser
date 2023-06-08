//
//  HomeHotWebsiteView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

struct HomeHotWebsiteView: View {
    
    @ObservedObject var webData = HotWebsiteData()
    
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    var body: some View {
        
        HomeContentView(titleString: "热门网站", dataSources: webData.hotWebsites)
           // .environmentObject(homeViewModel)
    }
}

struct HomeHotWebsiteView_Previews: PreviewProvider {
    static var previews: some View {
        HomeHotWebsiteView()
    }
}
