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
    
    
    func getDomain() -> String {
        guard var domain = self.host else { return self.absoluteString }
        if domain.lowercased().hasPrefix("www.") {
            domain.removeFirst(4)
        }
        return domain
    }
    
    static func isValidURL(_ urlString: String) -> Bool {
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue),
              let match = detector.firstMatch(in: urlString,
                                              options: [],
                                              range: NSRange(location: 0, length: urlString.utf16.count)) else {
            return false
        }
        // it is a link, if the match covers the whole string
        return match.range.length == urlString.utf16.count
    }
}




struct URLValidator {

}

class URLGenerator {
    private let urlValidator: URLValidator
    
    init(urlValidator: URLValidator = .init()) {
        self.urlValidator = urlValidator
    }
    

}
