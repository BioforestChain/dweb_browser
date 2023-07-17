//
//  AppDataModel.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import Foundation

struct AppDataModel: Identifiable {
    
    var name: String
    var imageName: String
    var id = UUID().uuidString
}
