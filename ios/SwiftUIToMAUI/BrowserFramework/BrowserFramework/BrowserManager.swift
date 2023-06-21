//
//  BrowserViewController.swift
//  browser
//
//  Created by ui03 on 2023/5/31.
//

import CoreFoundation
import CoreGraphics
import Foundation
import SwiftUI
import UIKit
import WebKit

@objc(BrowserManager)
public class BrowserManager: NSObject {
    @objc public var swiftView: UIView?
    
    public typealias initNewWebView = (WKWebViewConfiguration?) -> WKWebView
    
    public static var webviewGenerator: initNewWebView?
    
    @objc override public init() {
        super.init()
        let controller = UIHostingController(rootView: BrowserView()
            .environmentObject(AddrBarOffset())
            .environmentObject(TabState())
        )
        self.swiftView = controller.view
    }
        
    @objc public func addNewWkWebView(webView: WKWebView) {
        addWebViewPublisher.send(webView)
    }
        
    @objc public func openWebViewUrl(urlString: String) {
        let url = URL(string: urlString)
        WebCacheMgr.shared.store = [WebCache(icon: URL.defaultSnapshotURL, lastVisitedUrl: url!, title: "title of \(url!)", snapshotUrl: URL.defaultSnapshotURL)]
    }
    
    @objc public static func webviewGeneratorCallback(callback: @escaping initNewWebView) {
        webviewGenerator = callback
    }
}
