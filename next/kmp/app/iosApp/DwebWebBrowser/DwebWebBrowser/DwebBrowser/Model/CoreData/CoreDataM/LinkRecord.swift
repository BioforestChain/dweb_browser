//
//  SearchEngineViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

struct LinkRecord: Hashable, Codable, Identifiable {
    var link: String
    var websiteIcon: String
    var title: String
    var createdDate: Int64
    var sectionTime: String = ""
    var id = UUID()
    
    init(link: String = emptyLink, imageName: String = "baidu", title: String = "test link", createdDate: Int64 = currentTime64, sectionTime: String = "") {
        self.link = link
        self.websiteIcon = imageName
        self.title = title
        self.createdDate = createdDate
    }
    
    static var currentTime64: Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
    
    static var example = LinkRecord(title: "baidu")
}
