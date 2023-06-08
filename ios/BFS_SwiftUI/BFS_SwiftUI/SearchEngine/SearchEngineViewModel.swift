//
//  SearchEngineViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

class SearchEngineViewModel: ObservableObject {
    
    static let shared = SearchEngineViewModel()
    @Published var records: [LinkRecord] = []
    
    private let bookmarnManager = BookmarkCoreDataManager()
//    private let historyManager = HistoryCoreDataManager()
    
    
    func fetchRecordList(placeHolder: String) {
      
        if placeHolder.isEmpty {
            records = []
            return
        }
        
        var tmpList: [LinkRecord] = []
//        let historyList = historyManager.fetchHistoryData(with: placeHolder)
        let bookmarkList = bookmarnManager.fetchBookmarkData(with: placeHolder)

//        if historyList != nil {
//            tmpList = historyList!.map( { addImageType(with: "ico_menu_history_disabled", record: $0) })
//        }

        if bookmarkList != nil {
            tmpList += bookmarkList!.map( { addImageType(with: "ico_bottomtab_book_disabled", record: $0) })
        }
        records = tmpList.sorted(by: { $0.createdDate > $1.createdDate})
    }
    
    private func addImageType(with imageName: String, record: LinkRecord) -> LinkRecord {
        var link = record
        link.imageName = imageName
        return link
    }
}
