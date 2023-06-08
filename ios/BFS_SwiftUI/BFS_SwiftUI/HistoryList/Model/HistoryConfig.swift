//
//  HistoryConfig.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import Foundation

enum NoResultEnum {
    case bookmark
    case linkHistory
    case none
}

struct HistoryConfig: Hashable, Codable {
    
    var searchTitle: String = ""
    var noResultImageName: String = ""
    var title: String = ""
    var noResultTitle: String = ""
    
    init(_ type: NoResultEnum) {
        
        switch type {
        case .bookmark:
            generateBookmarkConfig()
        case .linkHistory:
            generateLinkHistoryConfig()
        default:
            break
        }
    }
    
    private mutating func generateBookmarkConfig() {
        self.searchTitle = "搜索书签"
        self.noResultImageName = "ico_blank_bookmark"
        self.title = "书签"
        self.noResultTitle = "暂无书签"
    }
    
    private mutating func generateLinkHistoryConfig() {
        self.searchTitle = "搜索历史记录"
        self.noResultImageName = "ico_blank_history"
        self.title = "历史记录"
        self.noResultTitle = "暂无记录"
    }
}
