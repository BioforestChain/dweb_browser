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
    var snapshotCancellable: AnyCancellable?
    let snapshotImageChangedPublisher = PassthroughSubject<Void, Never>()

    @Published var webIconUrl: URL // url to the source of somewhere in internet
    @Published var lastVisitedUrl: URL // the website that user has opened on webview
    @Published var shouldShowWeb: Bool

    @Published var title: String // page title
    @Published var snapshotImage = UIImage()
    @Published var snapshotUrl: URL // local file path is direct to the image has saved in document dir
    {
        didSet {
            snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
            snapshotImageChangedPublisher.send()
        }
    }
    
    init(icon: URL = URL.defaultWebIconURL, showWeb: Bool = false, lastVisitedUrl: URL = emptyURL, title: String = "起始页", snapshotUrl: URL = URL.defaultSnapshotURL) {
        shouldShowWeb = false
        webIconUrl = icon
        self.lastVisitedUrl = lastVisitedUrl
        self.title = title
        self.snapshotUrl = snapshotUrl
        snapshotImage = UIImage.snapshotImage(from: snapshotUrl)
        observeUrl()
        snapshotCancellable = snapshotImageChangedPublisher.sink {}
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
        snapshotCancellable = snapshotImageChangedPublisher.sink {}
    }
    
    deinit {
        snapshotCancellable?.cancel()
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

class WebCacheStore: ObservableObject {
    private let userdefaultKey = "userdefaultWebCache"
    @Published var caches: [WebCache] = []
    @Published var webWrappers: [WebWrapper] = []

    var cancellables = Set<AnyCancellable>()
    init() {
        loadCaches()
        caches.forEach { webCache in
            webCache.snapshotImageChangedPublisher
                .dropFirst()
                .sink { [weak self] _ in
                    self?.saveCaches()
                }
                .store(in: &cancellables)
        }
        
        $caches.sink { [weak self] webCaches in
                print("caches titles \(webCaches.map { $0.title })")
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
        cache.snapshotImageChangedPublisher
            .dropFirst()
            .sink { [weak self] _ in
                self?.saveCaches()
            }
            .store(in: &cancellables)
        
        caches.append(cache)
        saveCaches()
    }
    
    func remove(by cacheId: UUID) {
        guard let cache = caches.filter({ $0.id == cacheId }).first else { return }
        guard let index = caches.firstIndex(of: cache) else { return }
        UIImage.removeImage(with: cache.snapshotUrl)
        caches.remove(at: index)
        
        saveCaches()
    }

    var saveCacheTimes = 0
    func saveCaches() {
        saveCacheTimes += 1
        print("have saved times: \(saveCacheTimes)")
        let data = try? JSONEncoder().encode(caches)
        UserDefaults.standard.set(data, forKey: userdefaultKey)
    }
    
    func cache(at index: Int) -> WebCache {
        return caches[index]
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
                   
    
    private func loadCaches() {
        if let data = UserDefaults.standard.data(forKey: userdefaultKey) {
            if let items = try? JSONDecoder().decode([WebCache].self, from: data) {
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
