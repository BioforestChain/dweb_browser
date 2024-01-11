//
//  DwebWebBrowserProtocol.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/4.
//

import Foundation
import UIKit
import WebKit
import DwebPlatformIosKit

@objc public class WebBrowserViewSiteData: NSObject {
    @objc var title: String
    @objc var url: String
    @objc var id: Int64
    @objc var iconCreater: ()-> UIImage?
    @objc public init(id: Int64, title: String, url: String, iconCreater: @escaping ()-> UIImage?) {
        self.title = title
        self.url = url
        self.id = id
        self.iconCreater = iconCreater
    }
}

@objc public protocol WebBrowserViewWebObserableProtocol {
    @objc var icon: NSString { get }
}

@objc public protocol WebBrowserViewDelegate {
    func createDesktopLink(link:String, title: String, iconString:String, completionHandler: @escaping (NSError?)->Void)
    func recognizedScreenGestures()
    func openDeepLink(url: String)
    #warning("这是一个便捷的方法，避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用")
    func doAction(name: String, params: [String: String]?)
}

@objc public protocol WebBrowserViewDataSource: TracklessDataSource, BookMarkDataSource, HistoryDataSource, WebBrowserViewWebDataSource {    
    #warning("这是一个便捷的方法，避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用")
    func getDatas(for: String, params: [String: AnyObject]?) -> [String: AnyObject]?
}

// MARK: DwebWKWebView
@objc public protocol WebBrowserViewWebDataSource {
    typealias WebType = DwebWKWebView
    func getWebView() -> WebType
    func destroyWebView(web: WebType)
}

// MARK: trackless mode
@objc public protocol TracklessDataSource {
    var trackModel: Bool { get set }
}

// MARK: bookmark
@objc public protocol BookMarkDataSource {
    func loadBookmarks() -> [WebBrowserViewSiteData]?
    
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
    func loadHistorys() -> [String: [WebBrowserViewSiteData]]?
    
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
