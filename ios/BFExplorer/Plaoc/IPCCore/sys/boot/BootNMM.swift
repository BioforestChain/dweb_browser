//
//  BootNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/1.
//

import UIKit
import Vapor

class BootNMM: NativeMicroModule {

    private var registeredMmids = Set<String>()
    private var apiRouting: [String: Response] = [:]
    private let dwebServer = HTTPServer()
    
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
        
        let app = dwebServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "register") { request -> Response in
            let response = self.defineHandler(req: request) { (reque, ipc) in
                self.register(mmid: ipc.remote?.mmid ?? "")
            }
            return response
        }
        
        group.on(.GET, "unregister") { request -> Response in
            let response = self.defineHandler(req: request) { reque, ipc in
                self.unregister(mmid: ipc.remote?.mmid ?? "")
            }
            return response
        }
        
        
        Task {
            for mmid in registeredMmids {
                _ = nativeFetch(urlstring: "file://dns.sys.dweb/open?app_id=\(mmid.urlEncoder())")
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
    
    /*
    internal func defineHandler2( request: Request, handler: (Request) -> Any?) -> Response {
        
        var response: Response?
        let result = handler(request)
        if let res = result as? Response {
            response = res
        } else {
            var headers = HTTPHeaders()
            headers.add(name: "Content-Type", value: "application/json")
            
            let status = HTTPResponseStatus(statusCode: 200)
            
            let content = ChangeTools.tempAnyToString(value: result)
            if content != nil {
                let body = Response.Body.init(string: content!)
                
                response = Response(status: status, headers: headers, body: body)
            } else {
                let status = HTTPResponseStatus(statusCode: 500)
                let whitespace = NSCharacterSet.whitespacesAndNewlines
                let content = """
                            <p>${request.uri}</p>
                            <pre>${ex.message ?: "Unknown Error"}</pre>
                            """.trimmingCharacters(in: whitespace)
                let body = Response.Body.init(string: content)
                response = Response(status: status, headers: HTTPHeaders(), body: body)
            }
        }
        return response!
   
    }
    
    internal func defineHandler2(req: Request, handler: (URLRequest, Ipc) -> Any?) -> Response {
        
        return defineHandler2(request: req) { request in
//            return handler(request, Ipc())
        }
    }*/
}
