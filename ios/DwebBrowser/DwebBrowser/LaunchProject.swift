//
//  LaunchProject.swift
//  DwebBrowser
//
//  Created by ui06 on 5/29/23.
//

import Foundation
import UIKit
import WebKit
import SwiftUI

@objc
public class Entry: NSObject{
    public func rootviewcontroller(webviews: [WKWebView]) -> UIViewController{
        WebCacheMgr.shared.store = webviews.map({WebCache(icon: URL.defaultSnapshotURL, lastVisitedUrl: $0.url!, title: "title of \($0.url)", snapshotUrl: URL.defaultSnapshotURL)})
        if WebCacheMgr.shared.store.count == 0{
            WebCacheMgr.shared.store = [WebCache.example]
        }
        let vc = UIHostingController(rootView: BrowserView())
        return vc
    }
}
