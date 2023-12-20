//
//  SearchEngine.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

enum Engine: Int {
    case baidu
    case sogou
    case so360
}

struct Searcher: Identifiable {
    var id: Engine
    var name: String
    var icon: String
    var slogan: String
    var inputHandler: (String) -> String {
        return [baidu, sogou, so360][id.rawValue]
    }

    private func baidu(text: String) -> String {
        let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        return "https://m.baidu.com/s?word=\(query)"
    }

    private func sogou(text: String) -> String {
        let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        return "https://wap.sogou.com/web/searchList.jsp?keyword=\(query)"
    }

    private func so360(text: String) -> String {
        let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        return "https://m.so.com/s?q=\(query)"
    }

    static var baidu = Searcher(id: .baidu, name: "baidu", icon: "baidu", slogan: "百度一下，你就知道")
    static var sogou = Searcher(id: .sogou, name: "sogou", icon: "sougou", slogan: "上网从搜狗开始")
    static var so360 = Searcher(id: .so360, name: "360so", icon: "360so", slogan: "百毒不侵")
}

class WebSearcher: ObservableObject {
    static let shared = WebSearcher()
    var searchers: [Searcher] = [.baidu, .sogou, .so360] //360搜索有一个验证页面，会失败，然后跳转至最初的页面，并且进度条展示不正常
}

let webSearchers = WebSearcher.shared.searchers
