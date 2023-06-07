//
//  ShowAppView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct ShowAppView: View {
    
    var apps: [ShowAppModel] = []
    
    var body: some View {
        
        ShowAppContentView(titleString: "我的app", dataSources: apps)
    }
}

struct ShowAppView_Previews: PreviewProvider {
    static var previews: some View {
        ShowAppView()
    }
}
