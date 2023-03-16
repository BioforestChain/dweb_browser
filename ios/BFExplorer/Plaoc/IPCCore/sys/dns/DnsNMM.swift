//
//  DnsNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/1.
//

import UIKit
import Vapor
import SwiftSoup

class DnsNMM: NativeMicroModule {

//    private var mmConnectsMap: [MicroModule: [String: PromiseOut<ConnectResult>]] = [:]
    private var mmConnectsMap = NSMutableDictionary()
    private var runningApps: [String: MicroModule] = [:]
    private var installApps: [String: MicroModule] = [:]
    private var mmConnectsMapLock = NSLock()
    
    init() {
        super.init(mmid: "dns.sys.dweb")
    
    }
    
    func bootstrap() {
        bootstrapMicroModule(fromMM: self)
    }
    
    func connectTo(fromMM: MicroModule, toMmid: String, reason: Request) -> ConnectResult? {
    
        mmConnectsMapLock.withLock {
            var connectsMap = self.mmConnectsMap[fromMM] as? [String: PromiseOut<ConnectResult>]
            if connectsMap == nil {
                let map: [String: PromiseOut<ConnectResult>] = [:]
                connectsMap = map
                self.mmConnectsMap[fromMM] = map
            }
            
            var pro = connectsMap?[toMmid] as? PromiseOut<ConnectResult>
            if pro == nil {
                pro = PromiseOut<ConnectResult>()
            }
            DispatchQueue.global().async {
                if let toMM = self.open(mmid: toMmid) {
                    let connects = connectMicroModules(fromMM: fromMM, toMM: toMM, reason: reason)
                    pro!.resolver(connects)
                    _ = connects.ipcForFromMM.onClose { _ in
                        connectsMap?.removeValue(forKey: toMmid)
                    }
                }
            }
            return pro?.waitPromise()
        }
    }
    
    func bootstrapMicroModule(fromMM: MicroModule) {
        Task {
            fromMM.bootstrap(bootstrapContext: MyBootstrapContext(dns: MyDnsMicroModule(dnsMM: self, fromMM: fromMM)))
        }
    }
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        install(mm: self)
        runningApps[self.mmid] = self
        
        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
     
        _ = afterShutdownSignal.listen { _ in
            
            nativeFetchAdaptersManager.append { fromMM, request in
                if request.url.scheme == "file", ((request.url.host?.hasSuffix(".dweb")) != nil) {
                    let mmid = request.url.host ?? ""
                    let micro = self.installApps[mmid]
                    if micro != nil {
                        let connectResult = self.connectTo(fromMM: fromMM, toMmid: mmid, reason: request)
                        let response = connectResult?.ipcForFromMM.request(request: request) ?? Response(status: .badGateway, body: Response.Body(string: request.url.string))
                        return response
                    } else {
                        return nil
                    }
                }
                return nil
            }
        }
        
        routerHandler()
        
        _ = self.open(mmid: "boot.sys.dweb")
    }
    
    // 处理路由
    private func routerHandler() {
        // 保存路由处理方法，方便http和file协议请求调用
        let openRouteHandler: RouterHandler = { request, _ in
           
            guard let app_id = request.query[String.self, at: "app_id"] else {
                return false
            }
            
            _ = self.open(mmid: app_id)
            return true
        }
        let closeRouteHandler: RouterHandler = { request, _ in
            guard let app_id = request.query[String.self, at: "app_id"] else {
                return false
            }
            
            _ = self.close(mmid: app_id)
            return true
        }
        apiRouting["\(self.mmid)/open"] = openRouteHandler
        apiRouting["\(self.mmid)/close"] = closeRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HTTPServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["open", "close"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    override func _shutdown() throws {
        for (_, value) in installApps {
            try? value.shutdown()
        }
        installApps.removeAll()
    }
    
    /** 安装应用 */
    func install(mm: MicroModule) {
        installApps[mm.mmid] = mm
    }
    
    func uninstall(mm: MicroModule) {
        installApps.removeValue(forKey: mm.mmid)
    }
    
    /** 查询应用 */
    private func query(mmid: String) -> MicroModule? {
        return installApps[mmid]
    }
    /** 打开应用 */
    private func open(mmid: String) -> MicroModule? {
        var micro = runningApps[mmid]
        if micro == nil {
            micro = query(mmid: mmid)
            if micro != nil {
                bootstrapMicroModule(fromMM: micro!)
            } else {
                print("no found app: \(mmid)")
                return nil
            }
        }
        return micro
    }
    /** 关闭应用 */
    private func close(mmid: String) -> Int {
        let micro = runningApps[mmid]
        
        guard micro != nil else { return -1 }
        do {
            try micro?.shutdown()
            return 1
        } catch {
            return 0
        }
    }
}


