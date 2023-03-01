//
//  BootNMM.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/31.
//

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
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "open") { request in
            print("BootNMM \(self.mmid) \(request.url.path)")
            return true
        }
        group.on(.GET, "register") { request in
            return self.register(mmid: request.ipc?.remote.mmid)
        }
        group.on(.GET, "unregister") { request in
            return self.unregister(mmid: request.ipc?.remote.mmid)
        }
        
        Task {
            for mmid in registerdMmids {
                _ = nativeFetch(url: "file://dns.sys.dweb/open?app_id=\(mmid.encodeURIComponent())")
            }
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
        registerdMmids.insert(mmid!)
        return true
    }
    
    /**
     * 移除一个boot程序
     * TODO 这里应该有用户授权，取消开机启动
     */
    private func unregister(mmid: Mmid?) -> Bool {
        if mmid == nil {
            return false
        }
        
        registerdMmids.remove(mmid!)
        return true
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
