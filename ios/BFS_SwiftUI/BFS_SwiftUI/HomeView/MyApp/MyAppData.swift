//
//  MyAppData.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

class MyAppData: ObservableObject {
    
    @Published var apps: [WebModel] = []
    
    init() {
        
        apps = [WebModel(icon: "douyu", title: "碳元域", link: "http://www.baidu.com")]
        //从app列表数据库中取
    }
}
