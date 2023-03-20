//
//  MultiWebViewNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor

class MultiWebViewNMM: NativeMicroModule {
    
    static var controllerMap: [String: MultiWebViewControllerManager] = [:]
    
    static let activityClassList = [
        ActivityClass(mmid: "", controller: MutilWebViewViewController()),
        ActivityClass(mmid: "", controller: MutilWebViewViewController()),
        ActivityClass(mmid: "", controller: MutilWebViewViewController()),
        ActivityClass(mmid: "", controller: MutilWebViewViewController()),
        ActivityClass(mmid: "", controller: MutilWebViewViewController())
    ]
  
    
    init() {
        super.init(mmid: "mwebview.sys.dweb")
    }
    
    static func getCurrentWebViewController(mmid: String) -> MultiWebViewControllerManager? {
        return controllerMap[mmid]
    }
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        
        
        
    }
    
    private func routerHandler() {
        var subscribers: [Ipc: Set<String>] = [:]
        
        let openRouteHandler: RouterHandler = { request, ipc in
            let url = request.query[String.self, at: "url"] ?? ""
            let remoteMM = ipc?.asRemoteInstance()
            if remoteMM == nil {
                fatalError("mwebview.sys.dweb/open should be call by locale")
            }
            let webviewId = self.openDwebView(remoteMm: remoteMM!, urlString: url)
            if ipc != nil {
                var refs = subscribers[ipc!]
                if refs == nil {
                    let listSet = Set<String>()
                    subscribers[ipc!] = listSet
                }
                refs?.insert(webviewId)
            }
            
            return webviewId
        }
        
        let closeRouteHandler: RouterHandler = { request, ipc in
            let webviewId = request.query[String.self, at: "webview_id"]!
            
            return self.closeDwebView(remoteMmid: ipc!.remote?.mmid ?? "", webviewId: webviewId)
        }
        
        let reOpenRouteHandler: RouterHandler = { (request, ipc) -> ActivityClass? in
            let remoteMmid = ipc?.remote?.mmid ?? ""
            
            let activityClass = MultiWebViewNMM.activityClassList.first{ $0.mmid == remoteMmid }
            
            if activityClass != nil {
                guard let app = UIApplication.shared.delegate as? AppDelegate else { return nil }
                
                app.window = UIWindow(frame: UIScreen.main.bounds)
                app.window?.makeKeyAndVisible()
                app.window?.rootViewController = activityClass?.controller
            }
            
            return activityClass
        }
        
        apiRouting["\(self.mmid)/open"] = openRouteHandler
        apiRouting["\(self.mmid)/close"] = closeRouteHandler
        apiRouting["\(self.mmid)/reOpen"] = reOpenRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HTTPServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request in
            self.defineHandler(request: request)
        }
        for pathComponent in ["open", "close", "reOpen"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    override func _shutdown() throws {
        
    }
    
    func openMutilWebViewActivity(remoteMmid: String) {
        
    }
    
    func openDwebView(remoteMm: MicroModule,urlString: String) -> String {
        
        let remotemmid = remoteMm.mmid
        
        var controller = MultiWebViewNMM.controllerMap[remotemmid]
        
        if controller == nil {
            controller = MultiWebViewControllerManager(mmid: remotemmid, localeMM: self, remoteMM: remoteMm)
            MultiWebViewNMM.controllerMap[remotemmid] = controller
        }
        
        openMutilWebViewActivity(remoteMmid: remotemmid)
        controller!.waitActivityCreated()
        return controller!.openWebView(url: urlString).webviewId
    }
    
    func closeDwebView(remoteMmid: String, webviewId: String) -> Bool {
        let controller = MultiWebViewNMM.controllerMap[remoteMmid]
        if controller != nil {
            controller?.closeWebView(webviewId: webviewId)
            return true
        }
        return false
    }
}


struct ActivityClass {
    
    var mmid: String
    var controller: MutilWebViewViewController
    
    init(mmid: String, controller: MutilWebViewViewController) {
        self.mmid = mmid
        self.controller = controller
    }
}
