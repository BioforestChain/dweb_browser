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

    private var mmMap: [String:MicroModule] = [:]
    private var running_apps: [String: MicroModule] = [:]
    private let dwebServer = HTTPServer()
    
    init() {
        super.init(mmid: "dns.sys.dweb")
    
    }
    
    override func _bootstrap() throws {
        install(mm: self)
        running_apps[self.mmid] = self
        
        /// 对全局的自定义路由提供适配器
        /** 对等连接列表 */
        let connects: [MicroModule: [String:Ipc]] = [:]
        /**
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _ = afterShutdownSignal.listen { _ in
            
            let adapter = { (fromMM: MicroModule, request: URLRequest) -> Response? in
                if request.url?.scheme == "file", ((request.url?.host?.hasSuffix(".dweb")) != nil) {
                    let mmid = request.url?.host ?? ""
                    let micro = self.mmMap[mmid]
                    if micro != nil {
                        var ipcMap = connects[fromMM] ?? [:]
                        var ipc = ipcMap[mmid]
                        if ipc == nil {
                            let toMM = self.open(mmid: mmid)
                            ipc = toMM?.connect(from: fromMM)
                            _ = ipc?.onClose(cb: { _ in
                                _ = ipcMap.removeValue(forKey: mmid)
                            })
                        }
                        return ipc?.request(request: request)
                    } else {
                        return Response(status: .badGateway, body: Response.Body.init(string: request.url?.absoluteString ?? ""))
                    }
                }
                return nil
            }
            let generics = nativeFetchAdaptersManager.append(adapter: GenericsClosure(closure: adapter))
            return generics
        }
        
        let app = dwebServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "open") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                if let app_id = reque.query[String.self, at: "app_id"] {
                    _ = self.open(mmid: app_id)
                }
                return true
            }
            return response
        }
        
        group.on(.GET, "close") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                if let app_id = reque.query[String.self, at: "app_id"] {
                    _ = self.close(mmid: app_id)
                }
                return true
            }
            return response
        }
        
        _ = self.open(mmid: "boot.sys.dweb")
    }
    
    override func _shutdown() throws {
        for (_, value) in mmMap {
            try? value.shutdown()
        }
        mmMap.removeAll()
    }
    
    /** 安装应用 */
    func install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }
    /** 查询应用 */
    private func query(mmid: String) -> MicroModule? {
        return mmMap[mmid]
    }
    /** 打开应用 */
    private func open(mmid: String) -> MicroModule? {
        var micro = running_apps[mmid]
        if micro == nil {
            micro = query(mmid: mmid)
            if micro != nil {
                micro!.bootstrap()
            } else {
                print("no found app: \(mmid)")
                return nil
            }
        }
        return micro
    }
    /** 关闭应用 */
    private func close(mmid: String) -> Int {
        let micro = running_apps[mmid]
        
        guard micro != nil else { return -1 }
        do {
            try micro?.shutdown()
            return 1
        } catch {
            return 0
        }
    }
}
