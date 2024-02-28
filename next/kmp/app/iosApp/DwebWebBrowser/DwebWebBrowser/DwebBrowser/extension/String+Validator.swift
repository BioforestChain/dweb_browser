//
//  String+Validator.swift
//  DwebBrowser
//
//  Created by bfs-kingsword09 on 2023/7/31.
//

import Foundation

extension String {
    func isURL() -> Bool {
        // 只判断 host(长度1~63,结尾是.然后带2~6个字符如[.com]，没有端口判断)：val regex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}\$".toRegex()
        // 以 http 或者 https 或者 ftp 打头，可以没有
        // 字符串中只能包含数字和字母，同时可以存在-
        // 最后以 2~5个字符 结尾，可能还存在端口信息，端口信息限制数字，长度为1~5位
        let regex = "^((https?|ftp)://)?([a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,5}(:[0-9]{1,5})?(/.*)?)$"
        let regex2 = "((https?|ftp)://)(((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}(:[0-9]{1,5})?(/.*)?)"
        let predicate = NSPredicate(format: "SELF MATCHES %@", regex)
        let predicate2 = NSPredicate(format: "SELF MATCHES %@", regex2)

        return predicate.evaluate(with: self) || predicate2.evaluate(with: self)
    }
    
    /**
     * 判断字符串是否携带了scheme，true为携带，false为未携带
     */
    func matchesSchemePattern() -> Bool {
        let pattern = /\b[a-zA-Z]+:\/\/\b/
        let matches = self.ranges(of: pattern)
        
        return !matches.isEmpty
    }
    
    var trim: String {
        var result = self
        result = result.replacingOccurrences(of: SearcherPrefix.baidu.rawValue, with: "")
        result = result.replacingOccurrences(of: SearcherPrefix.sogou.rawValue, with: "")
        result = result.replacingOccurrences(of: SearcherPrefix.so360.rawValue, with: "")
        
        return result.removingPercentEncoding ?? result
    }
//    
//    func trim() -> String {
//        
//        var result = String(stringLiteral: self)
//        if result.contains("https://www.baidu.com/s?wd=") {
//            result = result.replacingOccurrences(of: "https://www.baidu.com/s?wd=", with: "")
//        }
//        
//        if result.contains("https://www.sogou.com/web?query=") {
//            result = result.replacingOccurrences(of: "https://www.sogou.com/web?query=", with: "")
//        }
//        
//        if result.contains("https://www.so.com/s?q=") {
//            result = result.replacingOccurrences(of: "https://www.so.com/s?q=", with: "")
//        }
//        
//        return result.removingPercentEncoding ?? result
//    }
}
