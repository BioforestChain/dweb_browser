//
//  LinkRecord.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import Foundation

struct LinkRecord: Hashable, Codable, Identifiable {
    
    var link: String
    var imageName: String
    var title: String
    var createdDate: Int64
    var sectionTime: String = ""
    var id = UUID()
}

struct Group<ID, ELement>: Identifiable where ID: Hashable {
    
    let id: ID
    let items: [ELement]
}
