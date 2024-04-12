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

class WebBrowserDefaultProvider: WebBrowserViewDelegate, WebBrowserViewDataSource {
    
    func requestCameraPermission(completed: @escaping (Bool) -> Void) {
        completed(false)
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
    
    func readFile(path: String, completed: (NSData?, NSError?) -> Void) {
        Log()
    }
    
    // MARK: trackless
    var trackModel: Bool = false {
        didSet {
            Log("track: \(trackModel)")
        }
    }
    
    // MARK: bookmarks
    func loadBookmarks() -> [WebBrowserViewSiteData]? {
        return nil
    }
    
    func addBookmark(title: String, url: String, icon: Data?) {
        Log("title:\(title)")
    }
    
    func removeBookmark(bookmark: Int64) {
        Log("bookmark:\(bookmark)")
    }
    
    // MARK: historys
    func loadHistorys() -> [String : [WebBrowserViewSiteData]]? {
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
    
    // MARK: download
    func loadAllDownloadDatas() -> [WebBrowserViewDownloadData]? {
        nil
    }
    
    func removeDownload(ids: [String]) {
        Log()
    }
    
    func addDownloadObserver(id: String, didChanged:@escaping (WebBrowserViewDownloadData) -> Void) {
        Log()
    }
    
    func removeAllDownloadObservers() {
        Log()
    }
    
    func pauseDownload(id: String) {
        Log()
    }
    
    func resumeDownload(id: String) {
        Log()
    }
    
    func localPathFor(id: String) -> String? {
        return nil
    }
    
}
