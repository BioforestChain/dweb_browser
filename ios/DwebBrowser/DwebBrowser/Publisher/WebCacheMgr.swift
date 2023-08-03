//
//  TabPageViewModel.swift
//  DwebBrowser
//
//  Created by ui06 on 5/5/23.
//

import Combine
import Foundation
import SwiftUI
import UIKit

// 打开新页面时
class WebCache: ObservableObject, Identifiable, Hashable, Codable, Equatable {
    enum CodingKeys: String, CodingKey {
        case id
        case webIconUrl
        case lastVisitedUrl
        case title
        case snapshotUrl
    }
    
    var id = UUID()
    private var cancellables = Set<AnyCancellable>()
    @Published var webIconUrl: URL // url to the source of somewhere in internet
    @Published var lastVisitedUrl: URL // the website that user has opened on webview
    @Published var shouldShowWeb: Bool

    @Published var title: String // page title
    @Published var snapshotUrl: URL // local file path is direct to the image has saved in document dir
    {
        didSet {
            WebCacheMgr.shared.saveCaches()
            snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
        }
    }

    @Published var snapshotImage = UIImage.defaultSnapShotImage
    
    init(icon: URL = URL.defaultWebIconURL, showWeb: Bool = false, lastVisitedUrl: URL = emptyURL, title: String = "", snapshotUrl: URL = URL.defaultSnapshotURL) {
        shouldShowWeb = false
        webIconUrl = icon
        self.lastVisitedUrl = lastVisitedUrl
        self.title = title
        self.snapshotUrl = snapshotUrl
        snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
        observeUrl()
    }
    
    required init(from decoder: Decoder) throws {
        shouldShowWeb = false
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        webIconUrl = try container.decodeIfPresent(URL.self, forKey: .webIconUrl) ?? URL.defaultWebIconURL
        lastVisitedUrl = try container.decodeIfPresent(URL.self, forKey: .lastVisitedUrl) ?? emptyURL
        title = try container.decode(String.self, forKey: .title)
        snapshotUrl = try container.decodeIfPresent(URL.self, forKey: .snapshotUrl) ?? URL.defaultSnapshotURL
        snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
        observeUrl()
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encodeIfPresent(webIconUrl, forKey: .webIconUrl)
        try container.encodeIfPresent(lastVisitedUrl, forKey: .lastVisitedUrl)
        try container.encode(title, forKey: .title)
        try container.encodeIfPresent(snapshotUrl, forKey: .snapshotUrl)
    }
    
    static func == (lhs: WebCache, rhs: WebCache) -> Bool {
        return lhs.id == rhs.id &&
            lhs.webIconUrl == rhs.webIconUrl &&
            lhs.lastVisitedUrl == rhs.lastVisitedUrl &&
            lhs.title == rhs.title &&
            lhs.snapshotUrl == rhs.snapshotUrl
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(webIconUrl)
        hasher.combine(lastVisitedUrl)
        hasher.combine(title)
        hasher.combine(snapshotUrl)
    }
    
    static var example: WebCache {
        WebCache(lastVisitedUrl: URL(string: "https://www.apple.com")!, title: "apple")
    }
    
    static var blank: WebCache {
        WebCache(lastVisitedUrl: emptyURL, title: "起始页")
    }
    
    func isBlank() -> Bool {
        return lastVisitedUrl == emptyURL
    }
    
    private func observeUrl() {
        $lastVisitedUrl
            .map { $0 != emptyURL }
            .sink(receiveValue: { [weak self] isAvailable in
                self?.shouldShowWeb = isAvailable
            })
            .store(in: &cancellables)
    }
}

class WebCacheMgr: ObservableObject {
    static let shared = WebCacheMgr()
    private let userdefaultKey = "userdefaultWebCache"
    @Published var store: [WebCache] = []

    init() {
        loadCaches()
    }
    
    func createOne() {
        let cache = WebCache()
        store.append(cache)
        saveCaches()
    }
    
    func remove(webCache: WebCache) {
        guard let index = store.firstIndex(of: webCache) else { return }
        UIImage.removeImage(with: webCache.snapshotUrl)
        let _ = withAnimation(.easeInOut) {
            store.remove(at: index)
        }
        saveCaches()
    }

    var saveCacheTimes = 0
    func saveCaches() {
        saveCacheTimes += 1
        print("have saved times: \(saveCacheTimes)")
        let data = try? JSONEncoder().encode(store)
        UserDefaults.standard.set(data, forKey: userdefaultKey)
    }
    
    static func cache(at index: Int) -> WebCache {
        return WebCacheMgr.shared.store[index]
    }
    
    func loadCaches() {
        if let data = UserDefaults.standard.data(forKey: userdefaultKey) {
            if let items = try? JSONDecoder().decode([WebCache].self, from: data) {
                store = items
            }
        }
        if store.count == 0 {
            store = [.blank]
        }
    }
}
