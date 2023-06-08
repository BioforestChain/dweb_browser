//
//  HotSearchModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/25.
//

import SwiftUI

struct HotSearchModel: Hashable, Codable, Identifiable {
    
    var link: String
    var title: String
    var index: Int
    var id = UUID()
    
    init(link: String, title: String, index: Int) {
        self.link = link
        self.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        self.index = index
    }
    
    init(hot: HotSearchEntity) {
        self.link = hot.link ?? ""
        self.title = hot.title ?? ""
        self.index = Int(hot.index)
        self.id = hot.id ?? UUID()
    }
}

