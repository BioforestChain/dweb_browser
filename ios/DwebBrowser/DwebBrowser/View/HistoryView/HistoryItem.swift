//
//  HistoryItem.swift
//  TableviewDemo
//
//  Created by ui06 on 4/4/23.
//

import Foundation

struct HistoryItem: Identifiable{
    let id = UUID()
    let title: String
    let url:String
    let date: Date

    init(title: String, url: String, date: Date) {
        self.title = title
        self.url = url
        self.date = date
    }
    
}

extension HistoryItem: Codable {
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case url
        case date
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(title, forKey: .title)
        try container.encode(url, forKey: .url)
        try container.encode(date, forKey: .date)
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
//        let id = try container.decode(UUID.self, forKey: .id)
        let title = try container.decode(String.self, forKey: .title)
        let url = try container.decode(String.self, forKey: .url)
        let date = try container.decode(Date.self, forKey: .date)
        self.init(title: title, url: url, date: date)
    }
}
