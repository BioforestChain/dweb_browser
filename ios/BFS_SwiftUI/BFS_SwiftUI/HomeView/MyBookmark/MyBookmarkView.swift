//
//  MyBookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

struct MyBookmarkView: View {
    
    @ObservedObject var bookmarkData = MyBookmarkData()
    
    var body: some View {
        
        HomeContentView(titleString: "我的书签", dataSources: bookmarkData.bookmarks)
    }
}

struct MyBookmarkView_Previews: PreviewProvider {
    static var previews: some View {
        MyBookmarkView()
    }
}
