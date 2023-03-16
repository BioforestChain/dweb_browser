//
//  NativeMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Vapor

typealias RouterHandler<T> = (_ request: Request, _ ipc: Ipc?) -> T?

class NativeMicroModule: MicroModule {
    
    var reqidRouting: [/* req_id */Int:String/* route */] = [:]
    
    var apiRouting: [/* route */String:RouterHandler<Any>] = [:]
    
    func createNativeMicro() {
        
        _ = connectAdapterManager.append { fromMM, toMM, reason in
            if let toMM = toMM as? NativeMicroModule {
                let channel = NativeMessageChannel<IpcMessage, IpcMessage>()
                let toNativeIpc = NativeIpc(port: channel.port1, remote: fromMM, role: .SERVER)
                let fromNativeIpc = NativeIpc(port: channel.port2, remote: toMM, role: .CLIENT)
                fromMM.beConnect(ipc: fromNativeIpc, reason: reason)
                toMM.beConnect(ipc: toNativeIpc, reason: reason)
                return ConnectResult(ipcForFromMM: fromNativeIpc, ipcForToMM: toNativeIpc)
            } else {
                return nil
            }
        }
    }
    
    init(mmid: String) {
        super.init()
        self.mmid = mmid
        
        _ = onConnect { clientIpc, req in
            _ = clientIpc.onRequest { request,ipc in
                guard let url = URL(string: request.urlString) else { return }
                self.reqidRouting[request.req_id] = url.host! + url.path
                let ipcRequest = request.toRequest()
                print("fetch", "NMM/Handler", ipcRequest.url)
                let res = self.defineHandler(request: ipcRequest, ipc: clientIpc)
                let ipcResMessage = IpcResponse.fromResponse(req_id: request.req_id, response: res, ipc: clientIpc)!.ipcResMessage
                clientIpc.postMessage(message: ipcResMessage)
            }
        }
    }
    
    // 对路由处理方法包裹一层，用于http路由
    func defineHandler(request: Request, ipc: Ipc? = nil) -> Response {
        let routeHandler = self.apiRouting[request.route!.path.string]!
        
        let result = routeHandler(request, ipc)
        
        if let result = result as? Response {
            return result
        } else if let result = result as? Codable {
            let content = ChangeTools.tempAnyToString(value: result)
            return Response(status: .ok, headers: .init([("Content-Type", "application/json")]), body: .init(string: content!))
        } else {
            return Response(status: .internalServerError, body: .init(string: """
                <p>\(request.url)</p>
                <pre>Unknow Error</pre>
            """.trimmingCharacters(in: .whitespacesAndNewlines)))
        }
    }
/*
    internal func defineHandler( request: Request, handler: (Request) -> Any?) -> Response {
        
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
    
    internal func defineHandler(req: Request, handler: (Request, Ipc) -> Any?) -> Response {
        
        return defineHandler(request: req) { request in
            return handler(request, Ipc())
        }
    }*/
}
