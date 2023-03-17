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
    
    override init() {
        super.init()
        self.mmid = "dns.sys.dweb"
    }
    
    func bootstrap() async {
        await bootstrapMicroModule(fromMM: self)
    }
    
    /** 对等连接列表 */
    private var mmConnectsMap: [MicroModule:[Mmid:PromiseOut<ConnectResult>]] = [:]
//    private var mmConnectsMap: [MicroModule:Mmid] = [:]
    private let mmConnectMapLock = NSLock()
    /** 为两个mm建立 ipc 通讯 */
    private func connectTo(fromMM: MicroModule, toMmid: Mmid, reason: Request) async -> ConnectResult {
        await mmConnectMapLock.withLock {
            /** 一个互联实例表 */
            let connectsMap = mmConnectsMap[fromMM]
            
            /**
             * 一个互联实例
             */
            if connectsMap == nil {
                mmConnectsMap[fromMM] = [:]
            }
            
            let po = PromiseOut<ConnectResult>()
            Task {
                
                let toMM = await self.open(mmid: toMmid)
                print("DNS/connect", "\(fromMM.mmid) => \(toMmid)")
                let connects = await connectMicroModules(fromMM: fromMM, toMM: toMM, reason: reason)
                po.resolve(connects)
                _ = connects.ipcForFromMM.onClose {
                    _ = self.mmConnectMapLock.withLock {
                        self.mmConnectsMap[fromMM]!.removeValue(forKey: toMmid)
                    }
                    
                    return .OFF
                }
            }
            
            return po
        }.waitPromise()
    }
    
    class MyBootstrapContext: BootStrapContext {
        var dns: DnsMicroModule
        init(dns: MyDnsMicroModule) {
            self.dns = dns
        }
    }
    
    class MyDnsMicroModule: DnsMicroModule {
        private let dnsMM: DnsNMM
        private let fromMM: MicroModule
        
        init(dnsMM: DnsNMM, fromMM: MicroModule) {
            self.dnsMM = dnsMM
            self.fromMM = fromMM
        }
        
        func install(mm: MicroModule) {
            dnsMM.install(mm)
        }
        
        func uninstall(mm: MicroModule) {
            dnsMM.uninstall(mm)
        }
        
        func connect(mmid: Mmid, reason: Request?) async -> ConnectResult {
            return await dnsMM.connectTo(
                fromMM: fromMM,
                toMmid: mmid,
                reason: reason ?? Request.new(url: "file://\(mmid)"))
        }
    }
    
    func bootstrapMicroModule(fromMM: MicroModule) async {
//        Task {
//            await fromMM.bootstrap(bootstrapContext:MyBootstrapContext(dns: MyDnsMicroModule(dnsMM: self, fromMM: fromMM)))
//        }
        await fromMM.bootstrap(bootstrapContext:MyBootstrapContext(dns: MyDnsMicroModule(dnsMM: self, fromMM: fromMM)))
    }
    
    override func _bootstrap(bootstrapContext: BootStrapContext) async throws {
        install(self)
        running_apps[mmid] = self
        
        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _ = _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { (fromMM, request) in
            if request.url.scheme == "file" && request.url.host != nil && request.url.host!.hasSuffix("dweb") {
                let mmid = request.url.host
                if mmid != nil {
                    let mm = self.mmMap[mmid!]
                    
                    if mm != nil {
                        let connectResult = await self.connectTo(fromMM: fromMM, toMmid: mmid!, reason: request)
                        
                        return await connectResult.ipcForFromMM.request(request: request)
                    }
                }
                
                return nil
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
            print("open/\(self.mmid)", request.url.path)
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
    }
    
    private var running_apps: [Mmid:MicroModule] = [:]
    
    /** 安装应用 */
    func install(_ mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }
    
    /** 卸载应用 */
    func uninstall(_ mm: MicroModule) {
        mmMap.removeValue(forKey: mm.mmid)
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
        
        await bootstrapMicroModule(fromMM: app!)
        
        running_apps[mmid] = app!
        
        return app!
    }
    
    /** 关闭应用 */
    private func close(mmid: Mmid) async -> Int {
        let app = running_apps.removeValue(forKey: mmid)
        
        if app != nil {
            await app!.shutdown()
            return 1
        }
        
        return -1
    }
}
