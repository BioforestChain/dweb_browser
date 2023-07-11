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
        if let url = URL(string: urlString), url.isValidURL {
            return url
        } else {
            let searchString = "https://www.baidu.com/s?wd=\(urlString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
            return URL(string: searchString)!
        }
    }

    var isValidURL: Bool {
        return UIApplication.shared.canOpenURL(self)
    }
}

extension URL {
    static var defaultSnapshotURL: URL {
        let bundle = Bundle(for: BridgeManager.self)
        return bundle.url(forResource: "resource.bundle/snapshot", withExtension: "png")!
    }

    static var defaultWebIconURL: URL {
        let bundle = Bundle(for: BridgeManager.self)
        return bundle.url(forResource: "resource.bundle/defWebIcon", withExtension: "png")!
    }
}
