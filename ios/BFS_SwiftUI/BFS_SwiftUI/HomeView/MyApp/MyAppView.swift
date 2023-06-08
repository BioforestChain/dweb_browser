//
//  MyAppView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

struct MyAppView: View {
    
    @ObservedObject var appData = MyAppData()
    
    var body: some View {
        
        HomeContentView(titleString: "我的app", dataSources: appData.apps)
    }
}

struct MyAppView_Previews: PreviewProvider {
    static var previews: some View {
        MyAppView()
    }
}
