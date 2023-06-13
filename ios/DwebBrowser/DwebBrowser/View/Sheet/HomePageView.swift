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
                        Image("defimage")
                            .padding(.horizontal, 80)
                        
                        Text("DwebBrowser")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(Color(hexString: "0A1626"))
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
