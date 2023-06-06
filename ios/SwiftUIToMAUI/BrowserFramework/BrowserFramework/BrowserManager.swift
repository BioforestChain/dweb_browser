//
//  BrowserViewController.swift
//  browser
//
//  Created by ui03 on 2023/5/31.
//

import UIKit
import WebKit
import SwiftUI

@objc(BrowserManager)
public class BrowserManager: NSObject {
    
    @objc public var swiftView: UIView?
    
    @objc public var webViewCount: Int = 1
    
    public typealias addNewWebView = () -> Void
    
    public typealias clickAppCallback = (String) -> Void
    
    private var callback: addNewWebView?
    
    private var clickCallback: clickAppCallback?
    
    @objc public override init() {
        super.init()
        let controller = UIHostingController(rootView: BrowserView()
            .environmentObject(AddrBarOffset())
            .environmentObject(TabState())
        )
        swiftView = controller.view
        
        webViewCount = 2 //从缓存中获取 当前已打开过几个webView
        
        _ = clickHomeAppPublisher.sink(receiveValue: { urlString in
            self.clickCallback?(urlString)
        })
        
        _ = clickAddButtonPublisher.sink(receiveValue: { _ in
            self.callback?()
        })
    }
    
    @objc public func fetchHomeData(param: [[String:String]], webViewList: [WKWebView]) {
        homeDataPublisher.send(param)
    }
    
    @objc public func addNewWkWebView(webView: WKWebView) {
        addWebViewPublisher.send(webView)
    }
    
    @objc public func showWebViewListData(list: [WKWebView]) {
        guard list.count > 0 else { return }
        WebCacheMgr.shared.store = list.map({WebCache(icon: URL.defaultSnapshotURL, lastVisitedUrl: $0.url!, title: "title of \($0.url!)", snapshotUrl: URL.defaultSnapshotURL)})
        if WebCacheMgr.shared.store.count == 0{
            WebCacheMgr.shared.store = [WebCache.example]
        }
    }
    
    @objc public func clickAppAction(callback: @escaping clickAppCallback) {
        self.clickCallback = callback
    }
    
    @objc public func clickAddNewHomePageAction(callback: @escaping addNewWebView) {
        self.callback = callback
    }
}
