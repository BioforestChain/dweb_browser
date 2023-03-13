//
//  DwebNativeComponent.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/30.
//

import WebKit
import MessageUI
import Foundation
import SwiftUI
import Vapor
import Combine

typealias RouterHandler<T> = (_ request: Request, _ ipc: Ipc?) async -> T?

class NativeMicroModule: MicroModule {
    internal var reqidRouting: [/* req_id */Int:String/* route */] = [:]
    internal var apiRouting: [/* route */String:RouterHandler<Any>] = [:]

    static var willInit: Void = {
        _ = connectAdapterManager.append { (fromMM, toMM, reason) in
            if let toMM = toMM as? NativeMicroModule {
                let channel = NativeMessageChannel<IpcMessage, IpcMessage>()
                let toNativeIpc = NativeIpc(port: channel.port1, remote: fromMM, role: .server)
                let fromNativeIpc = NativeIpc(port: channel.port2, remote: toMM, role: .client)
                await fromMM.beConnect(ipc: fromNativeIpc, reason: reason)
                await toMM.beConnect(ipc: toNativeIpc, reason: reason)
                return ConnectResult(ipcForFromMM: fromNativeIpc, ipcForToMM: toNativeIpc)
            } else {
                return nil
            }
        }
    }()
    
    override init() {
        // 静态初始化，只执行一次
        _ = NativeMicroModule.willInit
        
        super.init()
        mmid = ".sys.dweb"
        
        _ = onConnect { (clientIpc, _) in
            _ = clientIpc.onRequest { (ipcReqMessage, _) in
                let url = URI(string: ipcReqMessage.url)
                self.reqidRouting[ipcReqMessage.req_id] = url.host! + url.path
                
                let ipcRequest = ipcReqMessage.toIpcRequest(ipc: clientIpc)
                print("fetch", "NMM/Handler", ipcRequest.url)
                let res = await self.defineHandler(request: ipcRequest.toRequest(), ipc: clientIpc)
                let ipcResMessage = IpcResponse.fromResponse(req_id: ipcRequest.req_id, response: res, ipc: clientIpc).ipcResMessage
                await clientIpc.postMessage(message: ipcResMessage)
                return nil
            }
            return nil
        }
    }
    
    // 对路由处理方法包裹一层，用于http路由
    internal func defineHandler(request: Request, ipc: Ipc? = nil) async -> Response {
        let routeHandler = self.apiRouting[request.route!.path.string]!
        
        let result = await routeHandler(request, ipc)
        
        if let result = result as? Response {
            return result
        } else if let result = result as? Codable {
            return Response(status: .ok, headers: .init([("Content-Type", "application/json")]), body: .init(string: JSONStringify(result)!))
        } else {
            return Response(status: .internalServerError, body: .init(string: """
                <p>\(request.url)</p>
                <pre>Unknow Error</pre>
            """.trimmingCharacters(in: .whitespacesAndNewlines)))
        }
    }
}

//extension Vapor.Request {
//    struct createIpc {
//        static var _clientIpc: Ipc? = nil
//    }
//    
//    var ipc: Ipc? {
//        get {
//            return createIpc._clientIpc
//        }
//        set {
//            createIpc._clientIpc = newValue
//        }
//    }
//}
//
//extension RoutesBuilder {
//    func on<Response>(
//        _ method: HTTPMethod,
//        _ path: [PathComponent],
//        body: HTTPBodyStreamStrategy = .collect,
//        use closure: @escaping (Request, Ipc?) async throws -> Response
//    ) -> Route
//        where Response: AsyncResponseEncodable
//    {
//        let responder = AsyncBasicResponder { request  in
//            if case .collect(let max) = body, request.body.data == nil {
//                _ = try await request.body.collect(max: max?.value ?? request.application.routes.defaultMaxBodySize.value).get()
//                
//            }
//            return try await closure(request, request.ipc).encodeResponse(for: request)
//        }
//        let route = Route(
//            method: method,
//            path: path,
//            responder: responder,
//            requestType: Request.self,
//            responseType: Response.self
//        )
//        self.add(route)
//        return route
//    }
//}
