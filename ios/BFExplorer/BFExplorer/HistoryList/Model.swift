//
//  Model.swift
//  Challenge
//
//  Created by Nathaniel Whittington on 6/5/22.
//

import Foundation

struct User: Codable {
    let userId: Int?
    let id: Int?
    let title: String?
    let completed: Bool?
}

struct Play: Codable {
    let name: String?
}
