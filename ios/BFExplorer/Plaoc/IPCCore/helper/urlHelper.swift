//
//  urlHelper.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/14.
//

import UIKit

class urlHelper: NSObject {

    
    static func URL_BASE(url: String) -> String {
        
        if url.hasPrefix("http:")
           || url.hasPrefix("https:")
           || url.hasPrefix("file:")
           || url.hasPrefix("chrome-extension:") {
            return url
        }
        return "http://localhost"
    }
    
    static func parseUrl(urlString: Any, base: String) -> URL? {
        
        if urlString is URL {
            return urlString as? URL
        }
        
        guard urlString is String else { return nil }
        let baseString = urlHelper.URL_BASE(url: base)
        let firstURL = URL(string: urlString as! String)
        
        let url = URL(string: baseString)
        
        let scheme = url?.scheme ?? ""
        
//        if url == nil {
//
//        } else {
//            return url
//        }
//        var content = urlString as! String
//        if content.contains("?") {
//            let array = content.components(separatedBy: "?")
//            content = array.first ?? ""
//        }
        
        
        return URL(string: "")!
    }
    
    static func buildUrl(url: URL, ext: Ext) -> URL? {
        
        var resultURL: URL?
        var path: String = ""
        var query: String = ""
        if ext.pathname != nil {
            path = ext.pathname!
        }
        if (ext.search != nil) {
            if ext.search! is String {
                query = ext.search as? String ?? ""
            } else {
                //TODO
            }
        }
        return nil
    }
}

struct Ext {
    
    var search: Any?
    var pathname: String?
}
