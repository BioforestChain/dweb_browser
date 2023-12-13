//
//  DwebBrowserHistoryStore.swift
//  iosApp
//
//  Created by instinct on 2023/12/11.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import DwebShared
import Combine

final class DwebBrowserHistoryStore: ObservableObject {
    
    static let shared = DwebBrowserHistoryStore()
    
    private let service = DwebBrowserIosSupport().browserService
    
    @Published private(set) var sections: [DateGroup<String, BrowserWebSiteInfo>] = []
    @Published private(set) var hasMore: Bool = true
    
    private var off = 0
        
    func loadHistory() {
        let historys: [String: [BrowserWebSiteInfo]] = service.loadHistorys() as! [String: [BrowserWebSiteInfo]]
        DispatchQueue.main.async { [weak self] in
            self?.sections = historys.keys.sorted().reversed().map { key in
                let day = Int(key) ?? 0
                let date = Date(timeIntervalSince1970: TimeInterval(day * 24 * 3600))
                return DateGroup(id: date.historyTime(), items: historys[key] ?? [])
            }
        }
    }
    
    func loadNextHistorys() {
        service.loadMoreHistory(off: Int32(off)) { [weak self] e in
            guard let self = self else { return }
            let allDataCount = sections.count
            self.loadHistory()
            hasMore = false
            if self.sections.count > allDataCount {
                hasMore = true
                off += 7
            }
        }
    }
    
    func addHistoryRecord(title: String, url: String) {
        service.addHistory(title: title, url: url, icon: nil) { [weak self] e in
            self?.loadHistory()
        }
    }
    
    func removeHistoryRecord(at section: DateGroup<String, BrowserWebSiteInfo>, indexSet: IndexSet) {
        let ids: [Int64] = indexSet.compactMap { index in
            guard section.items.count > index else { return nil }
            return section.items[index].id
        }
        ids.forEach { id in
            service.removeHistory(history: id) { e in
            }
        }
        section.remove(index: indexSet)
    }
}

extension BrowserWebSiteInfo: Identifiable {
    
}