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
    
    @objc public var webViewList: [WKWebView]? {
        didSet {
            guard webViewList != nil, webViewList!.count > 0 else { return }
            WebCacheMgr.shared.store = webViewList!.map({WebCache(icon: URL.defaultSnapshotURL, lastVisitedUrl: $0.url!, title: "title of \($0.url!)", snapshotUrl: URL.defaultSnapshotURL)})
            if WebCacheMgr.shared.store.count == 0{
                WebCacheMgr.shared.store = [WebCache.example]
            }
            if let handler = onValueChanged {
                handler(webViewList!)
            }
        }
    }
    
    @objc public var onValueChanged: (([WKWebView]) -> Void)?
    
    @objc public override init() {
        super.init()
        let controller = UIHostingController(rootView: BrowserView()
            .environmentObject(AddrBarOffset())
            .environmentObject(TabState())
        )
        swiftView = controller.view
    }
}
