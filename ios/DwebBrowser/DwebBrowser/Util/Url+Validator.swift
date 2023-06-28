//
//  Url+Validator.swift
//  DwebBrowser
//
//  Created by ui06 on 5/15/23.
//

import Foundation
import UIKit
//import FaviconFinder

extension URL {

    static func makeURL(from text: String) -> URL? {
        let text = text.lowercased()
        guard isValidURL(text) else {
            return getSearchURL(for: text)
        }
        
        guard text.hasPrefix("http://") || text.hasPrefix("https://") else {
            return URL(string: "http://\(text)")
        }
        
        return URL(string: text)
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
    
    static private func getSearchURL(for text: String) -> URL? {
        guard let encodedSearchString = text.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed) else {
            return nil
        }
        let baiduSearchLink = "https://www.baidu.com/s?wd=\(encodedSearchString)"

        return URL(string: baiduSearchLink)
    }
}


extension URL{
    static var defaultSnapshotURL: URL{
        Bundle.main.url(forResource: "snapshot", withExtension: "png")!
    }
    
    static var defaultWebIconURL: URL {
        Bundle.main.url(forResource: "defWebIcon", withExtension: "png")!
    }
    
    
//    static func downloadWebsiteIcon(iconUrl: URL, with completion:@escaping (URL)->Void) {
//        Task{
//            do {
//                let favicon = try await FaviconFinder(url: iconUrl).downloadFavicon()
//
////                print("URL of Favicon: \(favicon.url)")
////                DispatchQueue.main.async {
//                    completion(favicon.url)
////                }
//            } catch let error {
//                print("Error: \(error)")
//            }
//        }
//    }
}
    
