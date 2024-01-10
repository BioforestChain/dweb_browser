//
//  File.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/4.
//

import Foundation
import UIKit
import WebKit
import DwebPlatformIosKit

class WebBrowserDefaultWebData: NSObject, WebBrowserViewDataProtocol {
    var title: String = ""
    var url: String = ""
    var id: Int64 = 0
    
    func iconUIImage() -> UIImage? {
        return nil
    }
}

class WebBrowserDefaultProvider: WebBrowserViewDelegate, WebBrowserViewDataSource {
        
    func getIconUIImage(data: WebBrowserViewDataProtocol) -> UIImage? {
        return nil
    }
    
    func getWebView() -> WebType {
        return DwebWKWebView(frame: .zero)
    }
    
    func destroyWebView(web: WebType) {
        Log()
    }
    
    func doAction(name: String, params: [String : String]?) {
        
    }
    
    func getDatas(for: String, params: [String : AnyObject]?) -> [String : AnyObject]? {
        return nil
    }
    
    func getWebBrowserViewDataClass() -> String {
        return NSStringFromClass(WebBrowserDefaultWebData.self)
    }
    
    init(trackModel: Bool) {
        self.trackModel = trackModel
    }
    
    // MARK: delegate
    func createDesktopLink(link: String, title: String, iconString: String, completionHandler: (NSError?) -> Void) {
        Log("title: \(title)")
    }
    
    func openDeepLink(url: String) {
        Log("url:\(url)")
    }
    
    func recognizedScreenGestures() {
        Log()
    }
    
    // MARK: trackless
    var trackModel: Bool = false {
        didSet {
            Log("track: \(trackModel)")
        }
    }
    
    // MARK: bookmarks
    func loadBookmarks() -> [WebBrowserViewDataProtocol]? {
        return nil
    }
    
    func addBookmark(title: String, url: String, icon: Data?) {
        Log("title:\(title)")
    }
    
    func removeBookmark(bookmark: Int64) {
        Log("bookmark:\(bookmark)")
    }
    
    // MARK: historys
    func loadHistorys() -> [String : [WebBrowserViewDataProtocol]]? {
        Log()
        return nil
    }
    
    func loadMoreHistory(off: Int32, completionHandler: (NSError?) -> Void) {
        Log()
    }
    
    func addHistory(title: String, url: String, icon: Data?, completionHandler: (NSError?) -> Void) {
        Log("title: \(title)")
    }
    
    func removeHistory(history: Int64, completionHandler: (NSError?) -> Void) {
        Log("histroy: \(history)")
    }
    
}
