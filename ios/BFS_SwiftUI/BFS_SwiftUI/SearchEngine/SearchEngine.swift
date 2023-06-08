//
//  SearchEngine.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

enum Engine {
    case baidu
    case sogou
    case engine360
}


func searchContent(for engine: Engine = .baidu, text: String) -> String {
    
    switch engine {
    case .baidu:
        return baiduEngine(text: text)
    case .sogou:
        return sogouEngine(text: text)
    case .engine360:
        return engine360(text: text)
    }
}

private func baiduEngine(text: String)  -> String {
    
    let urlString = "https://www.baidu.com/s"
    var components = URLComponents(string: urlString)
    components?.queryItems = [URLQueryItem(name: "wd", value: text)]
    return components?.url?.absoluteString ?? ""
}

private func sogouEngine(text: String) -> String {
    
    let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
    return "https://www.sogou.com/web?query=\(query)"
}

private func engine360(text: String) -> String {
    
    let query = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
    return "https://www.so.com/s?q=\(query)"
}
