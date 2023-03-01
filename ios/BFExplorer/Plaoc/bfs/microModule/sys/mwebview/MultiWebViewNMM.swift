//
//  MultiWebViewNMM.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/31.
//

import UIKit
import Foundation
import Vapor

class MultiWebViewNMM: NativeMicroModule {
    override init() {
        super.init()
        mmid = "mwebview.sys.dweb"
    }
    
    override func _bootstrap() async throws {
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "open") { request in
            guard let origin = request.query[String.self, at: "origin"] else {
                return Response(status: .badRequest)
            }
            let processId = request.query[String.self, at: "process_id"]
            
            let webViewId = self.openDwebView(origin: origin, processId: processId)
            
            return Response(status: .ok, body: .init(string: webViewId))
        }
        group.on(.GET, "close") { request in
            let processId = request.query[String.self, at: "process_id"]
            
            self.closeDwebView(processId: processId)
            
            return true
        }
    }
    
    func openDwebView(origin: String, processId: String?) -> String {
        return ""
    }
    
    func closeDwebView(processId: String?) {}
}

//class MultiWebViewNMM: NativeMicroModule {
//    var viewTree: ViewTree = ViewTree()
////    var Routers: [String:(Any) -> Any] = [:]
//
//    convenience init() {
//        self.init(mmid: "mwebview.sys.dweb")
//        Routers["/open"] = { args in
//            guard let args = args as? [String:Any] else { return false }
//
//            return self.open(args: args)
//        }
//    }
//
//    private func open(args: WindowOptions) -> Int {
//        let webview = WebViewViewController()
//        webview.urlString = args["url"] as! String
//
//        let webviewNode = viewTree.createNode(webview: webview, args: args)
//        viewTree.appendTo(webviewNode: webviewNode)
//
//        NotificationCenter.default.post(name: openAnAppNotification, object: webview)
//        print("id: \(webviewNode.id)")
//
//        return webviewNode.id
//    }
//
//    override func _shutdown() -> Any {
//        return true
//    }
//}
