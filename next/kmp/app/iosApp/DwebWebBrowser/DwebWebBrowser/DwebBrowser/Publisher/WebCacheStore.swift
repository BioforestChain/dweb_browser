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
import Observation

@Observable
class WebCache: Identifiable, Hashable, Codable, Equatable {

    var id = UUID()
    var webIconUrl: URL // url to the source of somewhere in internet
    var lastVisitedUrl: URL
    var title: String // page title
    var snapshotImage = lightSnapshotImage {
        didSet{
            snapshotChangedHandler()
        }
    }
    var snapshotUrl: URL // local file path is direct to the image has saved in document dir
    {
        didSet {
            snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
        }
    }
    var snapshotChangedHandler: () -> Void = {}
    var isWebVisible: Bool { lastVisitedUrl != emptyURL }

    init(icon: URL = URL.defaultWebIconURL, showWeb: Bool = false, lastVisitedUrl: URL = emptyURL, title: String = "起始页", snapshotUrl: URL = URL.defaultSnapshotURL) {
        webIconUrl = icon
        self.lastVisitedUrl = lastVisitedUrl
        self.title = title
        self.snapshotUrl = snapshotUrl
        snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
    }
    
    required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        webIconUrl = try container.decodeIfPresent(URL.self, forKey: .webIconUrl) ?? URL.defaultWebIconURL
        lastVisitedUrl = try container.decodeIfPresent(URL.self, forKey: .lastVisitedUrl) ?? emptyURL
        title = try container.decode(String.self, forKey: .title)
        snapshotUrl = try container.decodeIfPresent(URL.self, forKey: .snapshotUrl) ?? URL.defaultSnapshotURL
        snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
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
    

    enum CodingKeys: String, CodingKey {
        case id
        case webIconUrl
        case lastVisitedUrl
        case title
        case snapshotUrl
    }
}

class WebCacheStore: ObservableObject {
    private let userdefaultKey = "userdefaultWebCache"
    @Published var caches: [WebCache] = []{
        didSet{
            saveCaches()
        }
    }
    @Published var webWrappers: [WebWrapper] = []

    var cancellables = Set<AnyCancellable>()
    init() {
        loadCaches()
        
        $caches.sink { [weak self] webCaches in
                Log("caches titles \(webCaches.map { $0.title })")
                let cacheIds = webCaches.map { $0.id }
                let newStore = cacheIds.map {
                    self?.webWrapper(of: $0) ?? WebWrapper(cacheID: $0)
                }
                self?.webWrappers = newStore
            }
            .store(in: &cancellables)
    }
    
    func createOne() {
        let cache = WebCache()
        cache.snapshotChangedHandler = saveCaches
        caches.append(cache)
        saveCaches()
    }
    
    func remove(by cacheId: UUID) {
        guard let cache = caches.filter({ $0.id == cacheId }).first else { return }
        guard let index = caches.firstIndex(of: cache) else { return }
        UIImage.removeImage(with: cache.snapshotUrl)
        caches.remove(at: index)
        if caches.count == 0 {
            createOne()
        }
        saveCaches()
    }

    var saveCacheTimes = 0
    func saveCaches() {
        saveCacheTimes += 1
        Log("have saved times: \(saveCacheTimes)")
        let data = try? JSONEncoder().encode(caches)
        UserDefaults.standard.set(data, forKey: userdefaultKey)
    }
    
    func cache(at index: Int) -> WebCache {
        let webCache = caches[index]
        return webCache
    }
    
    func index(of cache: WebCache) -> Int? {
        return caches.firstIndex(of: cache)
    }
    
    var cacheCount: Int {
        return caches.count
    }
    
    func webWrapper(at index: Int) -> WebWrapper {
        let cache = caches[index]
        return webWrapper(of: cache.id)
    }
    
    func animateSnapshot(index: Int, colorScheme:  ColorScheme) -> UIImage{
        guard index >= 0, index < cacheCount else { return lightSnapshotImage }
        // TODO: imge 和url没有保持同步
        let cache = caches[index]
        let imageUrlString = cache.snapshotUrl.absoluteString
        
        var image = cache.snapshotImage
        if !imageUrlString.contains(webtag){
            if colorScheme == .light{
                if !imageUrlString.contains("light"){
                    image = lightSnapshotImage
                }
            } else if colorScheme == .dark {
                if !imageUrlString.contains("dark"){
                    image = darkSnapshotImage
                }
            }
        }
        return image
    }
    
    private func loadCaches() {
        if let data = UserDefaults.standard.data(forKey: userdefaultKey) {
            if let items = try? JSONDecoder().decode([WebCache].self, from: data) {
                let _ = items.map({ $0.snapshotChangedHandler = saveCaches })
                caches = items
            }
        }
        if caches.count == 0 {
            createOne()
        }
    }
    
    private func webWrapper(of cacheId: UUID) -> WebWrapper {
        if let wrapper = webWrappers.filter({ $0.id == cacheId }).first {
            return wrapper
        } else {
            let wrapper = WebWrapper(cacheID: cacheId)
            webWrappers.append(wrapper)
            return wrapper
        }
    }
}
