//
//  DwebWebBrowserProtocol.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/4.
//

import Foundation
import UIKit
import WebKit

@objc public protocol WebBrowserViewDataProtocol {
    @objc var title: String { get }
    @objc var url: String { get }
    @objc var id: Int64 { get }
    @objc func iconUIImage() -> UIImage?
}

@objc public protocol WebBrowserViewWebObserableProtocol {
    @objc var icon: NSString { get }
}

@objc public protocol WebBrowserViewTools {
    func getIconUIImage(data: WebBrowserViewDataProtocol) -> UIImage?
}

@objc public protocol WebBrowserViewDelegate {
    func createDesktopLink(link:String, title: String, iconString:String, completionHandler: @escaping (NSError?)->Void)
    func recognizedScreenGestures()
    func openDeepLink(url: String)
    
    //这是一个便捷的与kmp通讯的方法，用于避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用。
    func doAction(name: String, params: [String: String]?)
}

@objc public protocol WebBrowserViewDataSource: TracklessDataSource, BookMarkDataSource, HistoryDataSource, WebBrowserViewWebDataSource, WebBrowserViewTools {
    func getWebBrowserViewDataClass() -> String

    // 这是一个便捷的与kmp通讯的方法，用于避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用。
    func getDatas(for: String, params: [String: AnyObject]?) -> [String: AnyObject]?
}

// MARK: DwebWKWebView
@objc public protocol WebBrowserViewWebDataSource {
    typealias WebType = WKWebView & WebBrowserViewWebObserableProtocol
    func getWebView() -> WebType
    func destroyWebView(web: WKWebView)
}

// MARK: trackless mode
@objc public protocol TracklessDataSource {
    var trackModel: Bool { get set }
}

// MARK: bookmark
@objc public protocol BookMarkDataSource {
    func loadBookmarks() -> [WebBrowserViewDataProtocol]?
    
    func addBookmark(title: String, url: String, icon: Data?)
    
    func removeBookmark(bookmark:Int64)
}

extension BookMarkDataSource {
    func loadBookmarksToBrowser() -> [BrowserWebSiteInfo]? {
        guard let bookmarks = loadBookmarks() else { return nil }
        return bookmarks.map { BrowserWebSiteInfo($0) }
    }
}

// MARK: history
@objc public protocol HistoryDataSource {
    func loadHistorys() -> [String: [WebBrowserViewDataProtocol]]?
    
    func loadMoreHistory(off:Int32, completionHandler: @escaping (NSError?)->Void)
    
    func addHistory(title: String, url: String, icon: Data?, completionHandler: @escaping (NSError?)->Void)

    func removeHistory(history:Int64, completionHandler:@escaping (NSError?)->Void)
}

extension HistoryDataSource {
    func loadHistorysToBrowser() -> [String: [BrowserWebSiteInfo]]? {
        guard let history = self.loadHistorys() else { return nil }
        return history.mapValues { datas in
            return datas.map { BrowserWebSiteInfo($0) }
        }
    }
}
