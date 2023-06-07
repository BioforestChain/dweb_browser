//
//  HomePageView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct HomePageView: View {
    
    @ObservedObject var appData = ShowAppViewModel()
    
    var body: some View {
        
        if appData.apps.count > 0 {
            ShowAppContainerView(content: {
                ScrollView {
                    ShowAppView(apps: appData.apps)
                }
            }, viewModel: ShowHomePageViewModel())
        } else {
            emptyView()
        }
    }
    
    
    @ViewBuilder
    func emptyView() -> some View {
        
        VStack(alignment: .center, spacing: 0) {
            
            Image("empty_browser")
                .padding(.horizontal, 80)
                
            Text("DwebBrowser")
                .font(.system(size: 20, weight: .medium))
                .foregroundColor(Color(hexString: "0A1626"))
        }
    }
}

struct HomePageView_Previews: PreviewProvider {
    static var previews: some View {
        HomePageView()
    }
}
