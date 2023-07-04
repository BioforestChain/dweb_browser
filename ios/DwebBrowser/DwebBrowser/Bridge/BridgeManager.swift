//
//  BrowserView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/20.
//

import SwiftUI
import UIKit
import WebKit

@objc(BridgeManager)
public class BridgeManager: NSObject {
    static let shared = BridgeManager()
    @objc public var browserView: UIView?
    
    public typealias initNewWebView = (WKWebViewConfiguration?) -> BrowserWebview
    public static var webviewGenerator: initNewWebView?
    
    public typealias clickAppCallback = (String) -> Void
    private var clickCallback: clickAppCallback?
    
    @objc override public init() {
        super.init()
        
        let controller = UIHostingController(rootView: BrowserView()
            .environmentObject(AddrBarOffset())
            .environmentObject(ToolBarState())
            .environment(\.managedObjectContext, DataController.shared.container.viewContext))
        
        self.browserView = controller.view
    }

    @objc public func clickApp(appUrl: String) {
        self.clickCallback?(appUrl)
    }
    
    @objc public static func webviewGeneratorCallback(callback: @escaping initNewWebView) {
        self.webviewGenerator = callback
    }
    
    @objc public func clickAppAction(callback: @escaping clickAppCallback) {
        self.clickCallback = callback
    }
    
    @objc public func testWebView(webView: WKWebView) {
        print("test")
    }
}
