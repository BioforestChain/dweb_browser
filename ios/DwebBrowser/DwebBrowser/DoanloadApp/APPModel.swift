//
//  APPModel.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/5.
//

import Foundation

struct APPModel: Identifiable, Codable {
    
    var id = UUID().uuidString
    let server: [String: String]
    var icon: String
    var title: String
    var subtitle: String
    var introduction: String
    var images: [String]
    var downloadUrl: String
    var author: [String]
    var version: String
//    var newFeature: [String]
    var keywords: [String]
    var size: Int
    var home: String
    var fileHash: String
    var plugins: [String]
    var releaseDate: String
}
