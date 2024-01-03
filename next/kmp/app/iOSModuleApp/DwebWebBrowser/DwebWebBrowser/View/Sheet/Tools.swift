//
//  Tools.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/4.
//

import SwiftUI

//获取URL的host
func fetchURLHost(urlString: String) -> String {
    
    guard let url = URL(string: urlString),
          let host = url.host
    else { return "" }
    return host.hasPrefix("www.") ? host.replacingOccurrences(of: "www.", with: "") : host
}


func handleURLPrefix(urlString: String) -> String {
    
    if urlString.hasPrefix("http://www.") || urlString.hasPrefix("https://www.") {
        return urlString
    }
    if urlString.hasPrefix("www.") {
        return "https://" + urlString
    }
    return "https://www." + urlString
}

func paramURLAbsoluteString(with absolute: String) -> String {
    
    var result = absolute
    if absolute.contains("https://www.baidu.com/s?wd=") {
        
        result = absolute.replacingOccurrences(of: "https://www.baidu.com/s?wd=", with: "")
    }
    
    if absolute.contains("https://www.sogou.com/web?query=") {
        
        result = absolute.replacingOccurrences(of: "https://www.sogou.com/web?query=", with: "")
    }
    
    if absolute.contains("https://www.so.com/s?q=") {
        
        result = absolute.replacingOccurrences(of: "https://www.so.com/s?q=", with: "")
    }
    
    return result.removingPercentEncoding ?? result
}

//判断是否是URL格式

func isLink(urlString: String) -> Bool {
    
    do {
        let detector = try NSDataDetector(types: NSTextCheckingTypes(NSTextCheckingResult.CheckingType.link.rawValue))
        let res = detector.matches(in: urlString, options: NSRegularExpression.MatchingOptions(rawValue: 0), range: NSRange(location: 0, length: urlString.count))
        if res.count == 1 && res[0].range.location == 0 && res[0].range.length == urlString.count {
            return true
        }
    } catch {
        Log(error.localizedDescription)
    }
    
    return false
}

