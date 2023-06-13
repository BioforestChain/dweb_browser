//
//  BookmarkViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//
import SwiftUI

final class BookmarkViewModel: HistoryViewModel {
    
    @Published var dataSources: [LinkRecord] = []
    private let coredataManager = BookmarkCoreDataManager()
    override init() {
        super.init()
        DispatchQueue.main.async {
            self.loadHistoryData()
        }
    }

    private func loadHistoryData() {
        Task {
            dataSources = await coredataManager.fetchTotalBookmarks(offset: 0) ?? [.example]
        }
    }
    
    override func deleteSingleHistory(for uuid: String) {
        super.deleteSingleHistory(for: uuid)
        coredataManager.deleteBookmark(uuid: uuid)
    }
    
    override func groupedSep() {
        
    }
    
    override func loadMoreHistoryData()  {
        
        Task {
            await loadMoreBookmarkDataFromCoreData()
            DispatchQueue.main.async {
                self.groupedSep()
            }
        }
    }
    
    private func loadMoreBookmarkDataFromCoreData() async {
        guard let oldData = await coredataManager.fetchTotalBookmarks(offset: list.count),
              oldData.count > 0 else {
            return
        }
        list += oldData
    }
}

