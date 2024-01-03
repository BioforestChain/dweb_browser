//
//  BrowserView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/20.
//

import SwiftUI
import UIKit
import WebKit

// mike todo: import DwebPlatformIosKit


@objc(BridgeManager)
public class BridgeManager: NSObject {
   static let shared = BridgeManager()
   @objc public var browserView: UIView?

   let browserSubView = BrowserView()

   @StateObject var networkManager = NetworkManager()

    // mike todo: public typealias initNewWebView = (WKWebViewConfiguration?) -> DwebWKWebView
    // mike todo: public static var webviewGenerator: initNewWebView?

   public typealias clickAppCallback = (String) -> Void
   private var clickCallback: clickAppCallback?

   @objc override public init() {
       super.init()

       let controller = UIHostingController(rootView: browserSubView
           .environment(\.managedObjectContext, DataController.shared.container.viewContext)
           .environmentObject(self.networkManager))

       self.browserView = controller.view
   }

   @objc public func clickApp(appUrl: String) {
       self.clickCallback?(appUrl)
   }
    
// mike todo:
//   @objc public static func webviewGeneratorCallback(callback: @escaping initNewWebView) {
//       self.webviewGenerator = callback
//   }

   @objc public func clickAppAction(callback: @escaping clickAppCallback) {
       self.clickCallback = callback
   }

   @objc public func testWebView(webView: WKWebView) {
       Log("test")
   }
}
