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
        routerHandler()
    }
    
    private func routerHandler() {
        let openRouteHandler: RouterHandler = { request, ipc async in
            let url = request.query[String.self, at: "url"]!
            
            return self.openDwebView(remoteMmid: ipc!.remote.mmid, url: url)
        }
        let closeRouteHandler: RouterHandler = { request, ipc async in
            let webviewId = request.query[String.self, at: "webview_id"]!
            
            return self.closeDwebView(remoteMmid: ipc!.remote.mmid, webviewId: webviewId)
        }
        apiRouting["\(self.mmid)/open"] = openRouteHandler
        apiRouting["\(self.mmid)/close"] = closeRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["open", "close"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    func openDwebView(remoteMmid: Mmid, url: String?) -> String {
        return ""
    }
    
    func closeDwebView(remoteMmid: Mmid, webviewId: String) -> Bool {
        return true
    }
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
