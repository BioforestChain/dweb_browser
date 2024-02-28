//
//  Url+Validator.swift
//  DwebBrowser
//
//  Created by ui06 on 5/15/23.
//

import Foundation
import UIKit

extension URL {
    static func createUrl(_ urlString: String) -> URL {
        if var url = URL(string: urlString), urlString.isURL() || url.canBeOpen {
            if !urlString.matchesSchemePattern() || url.scheme == nil {
                url = URL(string: "https://" + urlString)!
            }
            return url
        } else {
            let searchString = SearcherPrefix.baidu.rawValue + (urlString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")
            return URL(string: searchString)!
        }
    }

    var canBeOpen: Bool {
        // 特殊处理dweb和about这两种schema, 用于打开内部页面
        if self.scheme == "dweb" {
            return true
        }
        return UIApplication.shared.canOpenURL(self)
    }
}

extension URL {
    static var lightSnapshot: URL {
        url(imageName: "snapshot_light")!
    }
    
    static var darkSnapshot: URL {
        url(imageName: "snapshot_dark")!
    }
    
    
    static var defaultSnapshotURL: URL {
        lightSnapshot
    }

    static var defaultWebIconURL: URL {
        url(imageName: "defWebIcon")!
    }
    
    func isNotDefSnapshotUrl() -> Bool {
        self != URL.lightSnapshot && self != URL.darkSnapshot
    }
    
    static func url(imageName: String) -> URL? {
        return Bundle.browserResources.url(forResource: imageName, withExtension: "png")
    }

    var domain: String {
        guard var domain = self.host else { return self.absoluteString }
        if domain.lowercased().hasPrefix("www.") {
            domain.removeFirst(4)
        }
        return domain
    }
}
