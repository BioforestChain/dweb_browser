//
//  InstalledApp.swift
//  DwebBrowser
//
//  Created by ui06 on 6/12/23.
//

import Foundation

struct InstalledApp: Hashable, Codable, Identifiable {
    var id = UUID()
    var icon: String
    var title: String
    var link: String
    
    static var example = InstalledApp(icon: "douyu", title: "碳元域", link: "http://www.baidu.com")
}

class InstalledAppMgr: ObservableObject {
    static var shared = InstalledAppMgr()
    
    @Published var apps: [InstalledApp] = []// [InstalledApp.example]
    
    init() {
        //从app列表数据库中取
    }
}
