//
//  TabPageViewModel.swift
//  DwebBrowser
//
//  Created by ui06 on 5/5/23.
//

import Foundation
import UIKit
import SwiftUI
import FaviconFinder
import Combine

let websites = [
    "https://www.baidu.com",
    "https://www.163.com",
    "https://www.sohu.com",
    "https://www.yahoo.com",
    "https://www.douban.com",
    "https://www.zhihu.com",
]

//打开新页面时
class WebCache: ObservableObject, Identifiable, Hashable,Codable{
    enum CodingKeys: String, CodingKey {
        case id
        case webIcon
        case lastVisitedUrl
        case title
        case snapshot
    }
    
    public var id = UUID()
    @Published var webIcon: URL            // url to the source of somewhere in internet
    @Published var lastVisitedUrl: URL     //the website that user has opened on webview
    @Published var title: String            // page title
    @Published var snapshot: URL           //local file path is direct to the image has saved in document dir
    
    public init(icon: URL = URL.defaultWebIconURL, lastVisitedUrl: URL = testURL, title: String = "", snapshot: URL = URL.defaultSnapshotURL) {
        self.webIcon = icon
        self.lastVisitedUrl = lastVisitedUrl
        self.title = title
        self.snapshot = snapshot
    }
    
    required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        webIcon = try container.decodeIfPresent(URL.self, forKey: .webIcon) ?? URL.defaultWebIconURL
        lastVisitedUrl = try container.decodeIfPresent(URL.self, forKey: .lastVisitedUrl) ?? testURL
        title = try container.decode(String.self, forKey: .title)
        snapshot = try container.decodeIfPresent(URL.self, forKey: .snapshot) ?? URL.defaultSnapshotURL
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encodeIfPresent(webIcon, forKey: .webIcon)
        try container.encodeIfPresent(lastVisitedUrl, forKey: .lastVisitedUrl)
        try container.encode(title, forKey: .title)
        try container.encodeIfPresent(snapshot, forKey: .snapshot)
    }
    
    public static func == (lhs: WebCache, rhs: WebCache) -> Bool {
        return lhs.id == rhs.id
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

class WebCacheStore: ObservableObject{
    static let shared = WebCacheStore()
    @Published var store: [WebCache] = []
    let userdefaultKey = "userdefaultWebCache"
    
    init(){
        loadCaches()
    }
    
    func addCacheItem(cache: WebCache){
        store.append(cache)
        saveCaches()
    }
    
    func saveCaches() {
        let data = try? JSONEncoder().encode(store)
        UserDefaults.standard.set(data, forKey: userdefaultKey)
    }
    
    func loadCaches() {
        if let data = UserDefaults.standard.data(forKey: userdefaultKey){
            if let items = try? JSONDecoder().decode([WebCache].self, from: data){
                store = items
            }
        }
        if store.count == 0 {
            store = [
                WebCache(lastVisitedUrl: testURL),
                WebCache(lastVisitedUrl: URL(string: "https://www.apple.com")!)
            ]
        }
    }
}
