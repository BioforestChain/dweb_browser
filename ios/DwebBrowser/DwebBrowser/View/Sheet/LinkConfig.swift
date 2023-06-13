//
//  HistoryConfig.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import Foundation

struct LinkConfig{
    var title: String = ""
    var type: String = ""
    var imageName: String = ""
    var msg: String = ""
    
    static var bookmark = LinkConfig(title: "搜索书签", type: "书签", imageName: "ico_blank_bookmark", msg: "暂无书签" )
    static var history = LinkConfig(title: "搜索历史记录", type: "历史记录", imageName: "ico_blank_history", msg:"暂无记录" )
}
