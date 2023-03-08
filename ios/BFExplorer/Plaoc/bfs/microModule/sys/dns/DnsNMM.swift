//
//  DwebDNS.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/30.
//

import Foundation
import Vapor

class DnsNMM: NativeMicroModule {
    private var mmMap: [Mmid:MicroModule] = [:]
    /// 对全局的自定义路由提供适配器
    /** 对等连接列表 */
    private var connects: [MicroModule:[Mmid:Ipc]] = [:]
    
    override init() {
        super.init()
        self.mmid = "dns.sys.dweb"
    }
    
    override func _bootstrap() async throws {
        install(self)
        running_apps[mmid] = self
        
        /**
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _ = _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { (fromMM, request) in
            if request.url.scheme == "file" && request.url.host != nil && request.url.host!.hasSuffix("dweb") {
                let mmid = request.url.host!
                let mm = self.mmMap[mmid]
                
                if mm != nil {
                    var ipcMap = self.connects[fromMM]
                    if ipcMap == nil {
                        self.connects[fromMM] = [:]
                        ipcMap = [:]
                    }
                    
                    var ipc = ipcMap![mmid]
                    if ipc == nil {
                        let toMM = await self.open(mmid: mmid)
                        ipc = await toMM.connect(from: fromMM)
                        _ = ipc!.onClose {
                            ipcMap!.removeValue(forKey: mmid)
                            
                            return .OFF
                        }
                    }
                    
                    print("start request")
                    return await ipc!.request(request: request)
                } else {
                    return Response(status: .badGateway, body: .init(string: request.url.string))
                }
            } else {
                return nil
            }
        })
        
        // 处理路由
        routerHandler()
        
        // 启动 boot 模块
        _ = await open(mmid: "boot.sys.dweb")
    }
    
    // 处理路由
    private func routerHandler() {
        // 保存路由处理方法，方便http和file协议请求调用
        let openRouteHandler: RouterHandler = { request, _ async in
            _ = self._connectSignal.listen { clientIpc in
                _ = clientIpc.onRequest { IpcRequest in
                    return nil
                }
                
                return nil
            }
            
            guard let app_id = request.query[Mmid.self, at: "app_id"] else {
                return false
            }
            
            _ = await self.open(mmid: app_id)
            return true
        }
        let closeRouteHandler: RouterHandler = { request, _ async in
            guard let app_id = request.query[Mmid.self, at: "app_id"] else {
                return false
            }
            
            _ = await self.close(mmid: app_id)
            return true
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
    
    override func _shutdown() async throws {
        for (_, mm) in mmMap {
            await mm.shutdown()
        }
        mmMap.removeAll()
        connects.removeAll()
    }
    
    private var running_apps: [Mmid:MicroModule] = [:]
    
    /** 安装应用 */
    func install(_ mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }
    
    /** 查询应用 */
    private func query(mmid: Mmid) -> MicroModule? {
        return mmMap[mmid]
    }
    
    /** 打开应用 */
    private func open(mmid: Mmid) async -> MicroModule {
        var app = running_apps[mmid]
        if app != nil {
            return app!
        }
        
        app = query(mmid: mmid)
        
        if app == nil {
            fatalError("no found app: \(mmid)")
        }
        
        await app!.bootstrap()
        
        return app!
    }
    
    /** 关闭应用 */
    private func close(mmid: Mmid) async -> Int {
        let app = running_apps[mmid]
        
        if app != nil {
            await app!.shutdown()
            
            running_apps.removeValue(forKey: mmid)
            return 1
        }
        
        return -1
    }
}

//class DnsNMM: NativeMicroModule {
//    static let shared = DnsNMM()
//
//    var apps: [MMID: MicroModule] = [:]
//
//    private var bootNMM = BootNMM()
//    private var multiWebViewNMM = MultiWebViewNMM()
//    var httpServerNMM = HttpServerNMM()
//    var jsProcessNMM = JsProcessNMM()
//
//    convenience init() {
//        self.init(mmid: "dns.sys.dweb")
//        self.install(mm: bootNMM)
//        self.install(mm: multiWebViewNMM)
//        self.install(mm: httpServerNMM)
//        self.install(mm: jsProcessNMM)
//
//        // 注册桌面
//        let desktopJmm = NativeMicroModule(mmid: "desktop.sys.dweb")
//        print("desktopJmm")
//        self.install(mm: desktopJmm)
//    }
//
////    private var Routers: [String:(Any) -> Any] = [:]
//    override func _bootstrap() -> Any {
//        install(mm: self)
//        running_apps[mmid] = self
//
//        Routers["/install-js"] = { _ in
//            return
//        }
//        Routers["/open"] = { args in
//            guard let args = args as? [String:MMID] else { return false }
//
//            if args["app_id"] != nil {
//                self.open(mmid: args["app_id"]!)
//            }
//
//            return true
//        }
//        Routers["/close"] = { args in
//            guard let args = args as? [String:MMID] else { return false }
//
//            if args["app_id"] != nil {
//                self.close(mmid: args["app_id"]!)
//            }
//
//            return true
//        }
//
//        return open(mmid: "boot.sys.dweb")
//    }
//
//    func query(mmid: MMID) -> MicroModule? {
//        if apps.index(forKey: mmid) != nil {
//            return apps[mmid]
//        } else {
//            return nil
//        }
//    }
//
//    var running_apps: [MMID: MicroModule] = [:]
//    func open(mmid: MMID) -> MicroModule? {
//        var app: MicroModule
//        if running_apps.index(forKey: mmid) != nil {
//            app = running_apps[mmid]!
//        } else {
//            let mm = query(mmid: mmid)
//
//            if mm == nil {
//                print("no found app: \(mmid)")
//                return nil
//            }
//
//            running_apps[mmid] = mm!
//            mm!.bootstrap()
//            app = mm!
//        }
//
//        return app
//    }
//
//    func _shutdown() {
//        for mmid in running_apps.keys {
//            let _ = close(mmid: mmid)
//        }
//    }
//
//    func install(mm: MicroModule) {
//        apps[mm.mmid] = mm
//    }
//
//    func close(mmid: MMID) -> Int {
//        if running_apps.index(forKey: mmid) != nil {
//            let app = running_apps[mmid]!
//            app.shutdown()
//            return 0
//        } else {
//            return -1
//        }
//    }
//
//    private var connects: [MicroModule: [MMID:NativeIpc]] = [:]
//    // 原生fetch
//    func nativeFetch(urlString: String, microModule: MicroModule?) -> Any? {
//        guard let url = URL(string: urlString) else { return nil }
//
//        if url.scheme == nil {
//            return nil
//        }
//
//        if url.host == nil {
//            return nil
//        }
//
//        if url.scheme!.hasPrefix("file") && url.host!.hasSuffix(".dweb") {
//            let pathnames = url.pathComponents
//            let pathname = pathnames.joined(separator: "")
//
//            var args: [String:Any] = [:]
//            let hosts = url.host!.split(separator: ".")
//
//            // 获取url get参数，
//            args.merge(dict: url.urlParameters!)
//
//            // 获取 microModule mmid
//            let mmid: MMID
//            if hosts.count > 3 {
//                args["appKey"] = hosts.first
//                mmid = hosts[hosts.count-3..<hosts.count].joined(separator: ".")
//            } else {
//                mmid = url.host!
//            }
//
//            if microModule != nil {
//                var from_app_ipcs = connects[microModule!]
//                if from_app_ipcs == nil {
//                    from_app_ipcs = [:]
//                    connects[microModule!] = from_app_ipcs
//                }
//
//                let ipc = from_app_ipcs![mmid]
//                if ipc == nil {
//                    do {
//                        let app = self.open(mmid: mmid)
//                        if let app = app as? JsMicroModule {
//                            let ipc = try app.connect(from: microModule!)
//                            ipc?.onClose {
////                                from_app_ipcs?.removeValue(forKey: mmid)!
//                                self.connects[microModule!]?.removeValue(forKey: mmid)
//                            }
//                            from_app_ipcs![mmid] = ipc as? JsIpc
//                            connects[microModule!] = from_app_ipcs
//                        } else {
//                            let ipc = try app?.connect(from: microModule!)
//                            ipc?.onClose {
////                                from_app_ipcs?.removeValue(forKey: mmid)!
//                                self.connects[microModule!]?.removeValue(forKey: mmid)
//                            }
//                            from_app_ipcs![mmid] = ipc as? NativeIpc
//                            connects[microModule!] = from_app_ipcs
//                        }
//                    } catch {
//    //                    throw MicroModuleError.moduleError("DnsNMM nativeFetch error: \(error)")
//                        print("DnsNMM nativeFetch error: \(error)")
//                    }
//                }
//            }
//
//            guard let mm = DnsNMM.shared.apps[mmid] as? NativeMicroModule else { return nil }
//
//            for key in mm.Routers.keys {
//                if pathname.hasPrefix(key) {
//                    mm._initCommonIpcOnMessage()
//                    return mm.Routers[key]!(args)
//                }
//            }
//        }
//
//        return nil
//    }
//}
