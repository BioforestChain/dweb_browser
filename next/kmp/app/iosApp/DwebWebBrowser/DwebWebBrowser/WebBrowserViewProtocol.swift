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

@objc public protocol WebBrowserViewDelegate {
    func createDesktopLink(link:String, title: String, iconString:String, completionHandler: @escaping (NSError?)->Void)
    func recognizedScreenGestures()
    func openDeepLink(url: String)
    func readFile(path: String, completed: @escaping (NSData?, NSError?) -> Void)
    //warning("doAction(name:params:)便捷方法，避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用")
    func doAction(name: String, params: [String: String]?)
}

@objc public protocol WebBrowserViewDataSource: TracklessDataSource, BookMarkDataSource, HistoryDataSource, WebBrowserViewWebDataSource, PermissionDataSource, DownloadDataSource {
    //warning("getDatas(for:params:)便捷方法，避免每次修改protocol，kmp也必须重新跑脚本的繁琐流程。只在功能开发阶段，或者debug的时候使用")
    func getDatas(for: String, params: [String: AnyObject]?) -> [String: AnyObject]?
}

// MARK: Permission
@objc public protocol PermissionDataSource {
    func requestCameraPermission(completed: @escaping (Bool)->Void)
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

// MARK: download data

@objcMembers public class WebBrowserViewDownloadData: NSObject {
    let name: String
    let date: UInt64
    let size: UInt32
    let mime: String
    let id: String
    /// 0: init, 1：下载中，2：暂停，3：取消,  4:失败，5: 完成
    let status: UInt8
    let progress: Float //下载进度，只在status处于未完成时，有意义。
    let localPath: String? //只有下载完成后，才有值。
    public init(name: String, date: UInt64, size: UInt32, mime: String, status: UInt8, id: String, progress: Float, localPath: String?) {
        self.name = name
        self.date = date
        self.size = size
        self.mime = mime
        self.status = status
        self.id = id
        self.progress = progress
        self.localPath = localPath
    }
}

@objc public protocol DownloadDataSource {
    func loadAllDownloadDatas() -> [WebBrowserViewDownloadData]?
    func removeDownload(ids: [String])
    func addDownloadObserver(id: String, didChanged:@escaping (WebBrowserViewDownloadData) -> Void)
    func removeAllDownloadObservers()
    func pauseDownload(id: String)
    func resumeDownload(id: String)
    func localPathFor(id: String) -> String?
}

