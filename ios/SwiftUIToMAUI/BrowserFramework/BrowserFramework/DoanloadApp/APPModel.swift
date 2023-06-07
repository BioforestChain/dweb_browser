//
//  APPModel.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/5.
//

import Foundation

struct APPModel: Identifiable, Codable {
    
    var id = UUID().uuidString
    var server: [String: String]
    var dweb_deeplinks: [String]
    var icon: String
    var name: String
    var short_name: String
    var description: String
    var images: [String]
    var downloadUrl: String
    var author: [String]
    var version: String
    var categories: [String]
    var new_feature: [String]
    var bundle_size: Int
    var home: String
    var bundle_hash: String
    var plugins: [String]
    var release_date: String
    var permissions: [String]
}
