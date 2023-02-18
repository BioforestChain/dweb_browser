//
//  urlHelper.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/14.
//

import UIKit
import SwiftyJSON

let URL_BASE = "http://localhost"

class urlHelper: NSObject {

    
//    static func URL_BASE(url: String) -> String {
//
//        if url.hasPrefix("http:")
//           || url.hasPrefix("https:")
//           || url.hasPrefix("file:")
//           || url.hasPrefix("chrome-extension:") {
//            return url
//        }
//        return "http://localhost"
//    }
    
    static func parseUrl(urlString: Any, base: String = URL_BASE) -> URL? {
        
        guard urlString is String || urlString is URL else { return nil }
        
        var paramURLString = ""
        if urlString is URL {
            let paramURL = urlString as! URL
            paramURLString = paramURL.absoluteString
        }
        
        if urlString is String {
            paramURLString = urlString as! String
        }
        
        guard paramURLString.count > 0 else { return nil }
        
        let baseURL = URL(string: base)
        
        return URL(string: paramURLString, relativeTo: baseURL)?.absoluteURL
        
    }
    
    static func updateUrlOrigin(url: Any, new_origin: String) -> URL? {
        
        let newUrl = urlHelper.parseUrl(urlString: url)
        var urlString = newUrl?.absoluteString
        let origin = newUrl?.absoluteString.analysisURLFormat() ?? ""
        urlString = urlString?.replacingOccurrences(of: origin, with: new_origin)
        return URL(string: urlString ?? "")
    }
    
    static func buildUrl(url: URL, ext: Ext) -> URL? {
        
        var path: String = ""
        var query: String = ""
        
        if ext.pathname != nil {
            path = ext.pathname!
        }
        if (ext.search != nil) {
            if ext.search! is String {
                query = ext.search as? String ?? ""
            } else  if ext.search is [String: Any] {
                var tmpDict = ext.search as! [String: Any]
                for (key, value) in tmpDict {
                    if value is String {
                        
                    } else {
                        tmpDict[key] = ChangeTools.tempAnyToString(value:value)
                    }
                }
                query = ChangeTools.dicValueString(tmpDict) ?? ""
            }
        }
        var components = URLComponents(string: url.absoluteString)
        components?.path = path
        components?.query = query
        
        return components?.url
    }
    
}

struct Ext {
    
    var search: Any?
    var pathname: String?
}
