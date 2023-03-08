//
//  BootNMM.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/31.
//

import Foundation
import Vapor

class BootNMM: NativeMicroModule {
    override init() {
        super.init()
        mmid = "boot.sys.dweb"
    }
    
    convenience init(initMmids: Set<Mmid>?) {
        self.init()
        if initMmids != nil {
            registerdMmids = registerdMmids.union(initMmids!)
        }
    }
    
    override func _bootstrap() async throws {
        routerHandler()
        
        Task {
            for mmid in registerdMmids {
                _ = await nativeFetch(url: "file://dns.sys.dweb/open?app_id=\(mmid.encodeURIComponent())")
            }
        }
    }
    
    private func routerHandler() {
        let registerRouteHandler: RouterHandler = { request, ipc async in
            return self.register(mmid: ipc?.remote.mmid)
        }
        let unregisterRouteHandler: RouterHandler = { request, ipc async in
            return self.unregister(mmid: ipc?.remote.mmid)
        }
        apiRouting["\(self.mmid)/register"] = registerRouteHandler
        apiRouting["\(self.mmid)/unregister"] = unregisterRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["register", "unregister"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    private var registerdMmids: Set<Mmid> = []
    
    /**
     * 开机启动项注册表
     * TODO 这里需要从数据库中读取
     */
    private func register(mmid: Mmid?) -> Bool {
        if mmid == nil {
            return false
        }
        return registerdMmids.insert(mmid!).inserted
    }
    
    /**
     * 移除一个boot程序
     * TODO 这里应该有用户授权，取消开机启动
     */
    private func unregister(mmid: Mmid?) -> Bool {
        if mmid == nil {
            return false
        }
        
        return registerdMmids.remove(mmid!) != nil
    }
}

//class BootNMM: NativeMicroModule {
//    var registeredMmids: Set<String> = ["desktop.sys.dweb", "http.sys.dweb"]
//
////    private var Routers: [String:(Any) -> Any] = [:]
//    override func _bootstrap() -> Any {
//        for mmid in registeredMmids {
//            DnsNMM.shared.nativeFetch(urlString: "file://dns.sys.dweb/open?app_id=\(mmid)", microModule: self)
//        }
//
//        return true
//    }
//
//    convenience init() {
//        self.init(mmid: "boot.sys.dweb")
//        Routers["/register"] = { args in
//            guard let args = args as? [String:MMID] else { return false }
//
//            if args["app_id"] != nil {
//                self.register(mmid: args["app_id"]!)
//            }
//
//            return true
//        }
//        Routers["/unregister"] = { args in
//            guard let args = args as? [String:MMID] else { return false }
//
//            if args["app_id"] != nil {
//                self.unregister(mmid: args["app_id"]!)
//            }
//
//            return true
//        }
//    }
//
//    private func register(mmid: String) -> Bool {
//        registeredMmids.insert(mmid)
//        return true
//    }
//
//    private func unregister(mmid: String) -> Bool {
//        if registeredMmids.contains(mmid) {
//            registeredMmids.remove(mmid)
//        }
//
//        return true
//    }
//}
