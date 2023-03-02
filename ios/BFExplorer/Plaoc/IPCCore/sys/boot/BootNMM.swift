//
//  BootNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/1.
//

import UIKit
import RoutingKit
import Vapor

class BootNMM: NativeMicroModule {

    private var registeredMmids = Set<String>()
    private var apiRouting: [String: Response] = [:]
    
    init(initMmids: [String]? = nil) {
        super.init(mmid: "boot.sys.dweb")
        if initMmids != nil {
            for mid in initMmids! {
                registeredMmids.insert(mid)
            }
        }
        self.routers = [:]
    }
    
    override func _bootstrap() throws {
        
        routesRegister()
        routesUnregister()
        
        Task {
            for mmid in registeredMmids {
                nativeFetch(urlstring: "file://dns.sys.dweb/open?app_id=\(mmid.urlEncoder())")
            }
        }
    }
    
    override func _shutdown() throws {
        routers?.removeAll()
    }
    
    private func register(mmid: String) {
        guard mmid.count > 0 else { return }
        self.registeredMmids.insert(mmid)
    }

    private func unregister(mmid: String) {
        self.registeredMmids.remove(mmid)
    }
    
    private func routesRegister() {
        routes(functionName: "/register", method: "GET")
    }
    
    private func routesUnregister() {
        routes(functionName: "/unregister", method: "GET")
    }
    
    private func routes(functionName: String, method: String) {
        
        switch (functionName,method) {
        case ("/register", "GET") :
            guard let url = URL(string: functionName) else { return }
            var request = URLRequest(url: url)
            request.httpMethod = method
            let response = defineHandler(req: request) { (reque, ipc) in
                self.register(mmid: ipc.remote?.mmid ?? "")
            }
            apiRouting["\(functionName)\(method)"] = response
        case ("/unregister", "GET") :
            guard let url = URL(string: functionName) else { return }
            var request = URLRequest(url: url)
            request.httpMethod = method
            let response = defineHandler(req: request) { (reque, ipc) in
                self.unregister(mmid: ipc.remote?.mmid ?? "")
            }
            apiRouting["\(functionName)\(method)"] = response
        default:
            break
        }
    }
}
