//
//  WebModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import Foundation

struct WebModel: Hashable, Codable, Identifiable {
    
    var id = UUID()
    var icon: String
    var title: String
    var link: String
}


