//
//  DwebBrowserHistoryStore.swift
//  iosApp
//
//  Created by instinct on 2023/12/11.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import Combine

class DateGroup<ID, ELement>: Identifiable where ID: Hashable  {
    
    let id: ID
    var items: [ELement] = []
    
    init(id: ID, items: [ELement]) {
        self.id = id
        self.items = items
    }
    
    func remove(index: IndexSet) {
        items.remove(atOffsets: index)
    }
}

final class DwebBrowserHistoryStore: ObservableObject {
    static let shared = DwebBrowserHistoryStore()
   
    @Published private(set) var sections: [DateGroup<String, BrowserWebSiteInfo>] = []
    @Published private(set) var hasMore: Bool = true
    
    private var off = 0
        
    func loadHistory() {
        guard let historys = browserViewDataSource.loadHistorysToBrowser() else { return }
        DispatchQueue.main.async { [weak self] in
            self?.sections = historys.keys.sorted().reversed().map { key in
                let day = Int(key) ?? 0
                let date = Date(timeIntervalSince1970: TimeInterval(day * 24 * 3600))
                return DateGroup(id: date.historyTime(), items: historys[key] ?? [])
            }
        }
    }
    
    func loadNextHistorys() {
        browserViewDataSource.loadMoreHistory(off: Int32(off)) { [weak self] e in
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
        browserViewDataSource.addHistory(title: title, url: url, icon: nil) { [weak self] e in
            self?.loadHistory()
        }
    }
    
    func removeHistoryRecord(at section: DateGroup<String, BrowserWebSiteInfo>, indexSet: IndexSet) {
        let ids: [Int64] = indexSet.compactMap { index in
            guard section.items.count > index else { return nil }
            return section.items[index].id
        }
        ids.forEach { id in
            browserViewDataSource.removeHistory(history: id) { e in
            }
        }
        section.remove(index: indexSet)
    }
}
