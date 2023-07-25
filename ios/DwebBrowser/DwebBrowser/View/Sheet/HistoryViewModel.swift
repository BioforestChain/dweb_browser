//
//  HistoryViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//


import SwiftUI

struct DateGroup<ID, ELement>: Identifiable where ID: Hashable {
    
    let id: ID
    let items: [ELement]
}


class HistoryViewModel: ObservableObject {
    @Published var sections: [DateGroup<String, LinkRecord>] = []
    private var searchText: String = ""
    
    var list: [LinkRecord] = [.example]
    
    var searchResults: [LinkRecord] {
        if searchText.isEmpty {
            return list
        } else {
            return list.filter { $0.title.contains(searchText) ||  $0.link.lowercased().contains(searchText.lowercased()) }
        }
    }
    
    func groupedSep() {
        let historyDict = Dictionary(grouping: searchResults, by: { $0.sectionTime})
        self.sections = historyDict.map { DateGroup(id: $0.key, items: $0.value.sorted(by: { $0.createdDate > $1.createdDate
        })) }.sorted(by: {$0.id > $1.id} )
    }
    
    //搜索
    func searchHistory(for searchText: String) {
        
        self.searchText = searchText
        groupedSep()
    }
    
    //删除数据
    func deleteSingleHistory(for uuid: String) {
        if let index = list.firstIndex(where: { $0.id.uuidString == uuid }) {
            list.remove(at: index)
            groupedSep()
        }
    }
    
    //删除数据
    func deleteHistory(for uuids: [String]) {
        list.removeAll { uuids.contains($0.id.uuidString) }
        groupedSep()
    }
    
    //分页加载
    func loadMoreHistoryData() { }
}


class HistoryMgr: ObservableObject {
    @Published var sections: [DateGroup<String, LinkRecord>] = []
    private var searchText: String = ""
    
    var list: [LinkRecord] = [.example]
    
    static var shared = HistoryMgr()
    private let coredataManager = HistoryCoreDataManager()
    
    init() {
        Task {
            list = await coredataManager.fetchTotalHistories(offset: 0) ?? []
            DispatchQueue.main.async {
                self.groupedSep()
            }
        }
    }
        
    func deleteHistory(for uuid: String) {
        if let index = list.firstIndex(where: { $0.id.uuidString == uuid }) {
            list.remove(at: index)
            groupedSep()
        }

        coredataManager.deleteHistory(uuid: uuid)
    }
    
    func loadMoreHistoryData()  {
        
        Task {
            await loadMoreLinkHistoryDataFromCoreData()
            DispatchQueue.main.async {
                self.groupedSep()
            }
        }
    }
    
    private func loadMoreLinkHistoryDataFromCoreData() async {
        guard let oldData = await coredataManager.fetchTotalHistories(offset: list.count),
              oldData.count > 0 else { return }
        list += oldData
    }
    

    var searchResults: [LinkRecord] {
        if searchText.isEmpty {
            return list
        } else {
            return list.filter { $0.title.contains(searchText) ||  $0.link.lowercased().contains(searchText.lowercased()) }
        }
    }
    
    func groupedSep() {
        let historyDict = Dictionary(grouping: searchResults, by: { $0.sectionTime})
        self.sections = historyDict.map { DateGroup(id: $0.key, items: $0.value.sorted(by: { $0.createdDate > $1.createdDate
        })) }.sorted(by: {$0.id > $1.id} )
    }
    
    //搜索
    func searchHistory(for searchText: String) {
        
        self.searchText = searchText
        groupedSep()
    }
    
    
    
}
