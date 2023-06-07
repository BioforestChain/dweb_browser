//
//  ShowAppModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import Foundation

struct ShowAppModel: Hashable, Codable, Identifiable {
    
    var id = UUID()
    var icon: String
    var title: String
    var link: String
}
