//
//  BrowserView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/20.
//

import UIKit
import SwiftUI
import WebKit

@objc(BridgeManager)
public class BridgeManager: NSObject {
    static let shared = BridgeManager()
    @objc public var browserView: UIView?
    
    public typealias initNewWebView = (WKWebViewConfiguration?) -> WKWebView
    public static var webviewGenerator: initNewWebView?
    
    public typealias clickAppCallback = (String) -> Void
    private var clickCallback: clickAppCallback?
    
    @objc public override init() {
        super.init()
        
        let controller = UIHostingController(rootView:  BrowserView()
            .environmentObject(AddrBarOffset())
            .environmentObject(ToolBarState())
            .environment(\.managedObjectContext, DataController.shared.container.viewContext))
        
        self.browserView = controller.view
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "OpenApp"), object: nil, queue: .main) { noti in
            guard let urlString = noti.object as? String else { return }
            self.clickCallback?(urlString)
        }
    }
    @objc public func clickApp(appUrl: String) {
        self.clickCallback?(appUrl)
    }
    
    @objc public static func webviewGeneratorCallback(callback: @escaping initNewWebView) {
        webviewGenerator = callback
    }
    
    @objc public func clickAppAction(callback: @escaping clickAppCallback) {
        self.clickCallback = callback
    }
}
