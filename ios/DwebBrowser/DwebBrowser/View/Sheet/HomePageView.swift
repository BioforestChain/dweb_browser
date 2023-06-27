//
//  HomePageView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI
struct HomePageView: View {
    var body: some View {
        ScrollView(.vertical){
            VStack {
                if !InstalledAppMgr.shared.apps.isEmpty {
                    InnerAppGridView()
                } else {
                    VStack {
                        Image("initialicon")
                            .resizable()
                            .frame(width: 210, height: 210)
                        Text("Dweb Browser")
                            .font(.system(size: 23, weight: .medium))
                        
                    }
                }
            }
        }
    }
}

struct HomePageView_Previews: PreviewProvider {
    static var previews: some View {
        HomePageView()
    }
}
