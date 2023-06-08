//
//  MyBookmarkData.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

class MyBookmarkData: ObservableObject {
    
    @Published var bookmarks: [WebModel] = []
    
    init() {
        bookmarks = [WebModel(icon: "douyu", title: "美国中文网站的数据来源", link: "http://www.baidu.com")]
        //从书签列表数据库中取
    }
}
