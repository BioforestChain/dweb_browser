//
//  LinkHistoryViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import SwiftUI

final class LinkHistoryViewModel: HistoryViewModel {
    
    private let coredataManager = HistoryCoreDataManager()
    
    override init() {
        super.init()
        Task {
            await loadHistoryData()
            DispatchQueue.main.async {
                self.groupedSep()
            }
        }
    }
    
    private func loadHistoryData() async {
        
        list = await coredataManager.fetchTotalHistories(offset: 0) ?? []
    }
    
    override func deleteSingleHistory(for uuid: String) {
        super.deleteSingleHistory(for: uuid)
        coredataManager.deleteHistory(uuid: uuid)
    }
    
    override func loadMoreHistoryData()  {
        
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
}
