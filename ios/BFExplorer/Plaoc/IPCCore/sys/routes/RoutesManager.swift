//
//  RoutesManager.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/1.
//

import UIKit
import Vapor

let baseURLString = "file://xxx.dewb"

class RoutesManager: NSObject {
    
    static let shared = RoutesManager()
    
    private(set) var registeredMmids = Set<String>()
    private(set) var apiRouting: [String: Response] = [:]
    
    func addMmid(mmids: [String]) {
        for mid in mmids {
            registeredMmids.insert(mid)
        }
    }
    
    func routes(urlString: String = baseURLString, functionName: String, method: String) {
        //TODO url传什么
        guard let url = URL(string: urlString) else { return }
        var request = URLRequest(url: url)
        request.httpMethod = method
        
        switch (functionName,method) {
        case ("/register", "GET") :
            let response = defineHandler(req: request) { (reque, ipc) in
                self.register(mmid: ipc.remote?.mmid ?? "")
            }
            apiRouting["\(functionName)\(method)"] = response
        case ("/unregister", "GET") :
            let response = defineHandler(req: request) { (reque, ipc) in
                self.unregister(mmid: ipc.remote?.mmid ?? "")
            }
            apiRouting["\(functionName)\(method)"] = response
        case ("/open", "GET") :
            let response = defineHandler(request: request) { reque in
                operateMonitor.routeMonitor.onNext((functionName, request))
                return true
            }
            apiRouting["\(functionName)\(method)"] = response
        case ("/close", "GET") :
            let response = defineHandler(request: request) { reque in
                operateMonitor.routeMonitor.onNext((functionName, request))
                return true
            }
            apiRouting["\(functionName)\(method)"] = response
        default:
            break
        }
    }
    
    private func register(mmid: String) {
        guard mmid.count > 0 else { return }
        self.registeredMmids.insert(mmid)
    }
    
    private func unregister(mmid: String) {
        self.registeredMmids.remove(mmid)
    }
    
    internal func defineHandler( request: URLRequest, handler: (URLRequest) -> Any?) -> Response {
        
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
    
    internal func defineHandler(req: URLRequest, handler: (URLRequest, Ipc) -> Any?) -> Response {
        
        return defineHandler(request: req) { request in
            return handler(request, Ipc())
        }
    }
}
