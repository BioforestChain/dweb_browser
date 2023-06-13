//
//  SearchEngine.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

enum Engine: Int{
    case baidu
    case sogou
    case so360
}

struct Searcher: Identifiable{
    var id: Engine
    var name: String
    var icon: String
    var slogan: String
    var inputHandler: (String) -> String {
        return [baidu,sogou,so360][id.rawValue]
    }
    
    private func baidu(text: String)  -> String {
        let urlString = "https://www.baidu.com/s"
        var components = URLComponents(string: urlString)
        components?.queryItems = [URLQueryItem(name: "wd", value: text)]
        return components?.url?.absoluteString ?? ""
    }
    
    private func sogou(text: String) -> String {
        
        let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        return "https://www.sogou.com/web?query=\(query)"
    }
    
    private func so360(text: String) -> String {
        
        let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        return "https://www.so.com/s?q=\(query)"
    }
    
    static var baidu = Searcher(id: .baidu, name: "baidu", icon: "defimage", slogan: "百度一下，你就知道")
    static var sogou = Searcher(id: .sogou, name: "sogou", icon: "defimage", slogan: "上网从搜狗开始")
    static var so360 = Searcher(id: .so360, name: "360so", icon: "defimage", slogan: "百毒不侵")

}

class WebSearcher: ObservableObject {
    static let shared = WebSearcher()
    var searchers: [Searcher] = [.baidu, .sogou, .so360]
}

