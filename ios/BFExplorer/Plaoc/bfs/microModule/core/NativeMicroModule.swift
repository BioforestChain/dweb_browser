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
    override init() {
        super.init()
        mmid = ".sys.dweb"
        
        _ = onConnect { clientIpc in
            _ = clientIpc.onRequest { message in
                let ipcRequest = message.0
                let url = URI(string: ipcRequest.url)
                self.reqidRouting[ipcRequest.req_id] = url.host! + url.path
                
                let res = await self.defineHandler(request: ipcRequest.toRequest(), ipc: clientIpc)
                let ipcResMessage = IpcResponse.fromResponse(req_id: ipcRequest.req_id, response: res, ipc: clientIpc).ipcResMessage
                await clientIpc.postMessage(message: ipcResMessage)
                return nil
            }
            return nil
        }
    }
    
    private var _connectedIpcSet: Set<Ipc> = []
    override func _connect(from: MicroModule) async -> Ipc {
        let channel = NativeMessageChannel<IpcMessage, IpcMessage>()
        let nativeIpc = NativeIpc(port: channel.port1, remote: from, role: .server)
        
        _connectedIpcSet.insert(nativeIpc)
        _ = nativeIpc.onClose {
            self._connectedIpcSet.remove(nativeIpc)
            return .OFF
        }
        await _connectSignal.emit((nativeIpc))
        return NativeIpc(port: channel.port2, remote: self, role: .client)
    }
    
    internal var _connectSignal = Signal<(NativeIpc)>()
    
    internal func onConnect(cb: @escaping ((NativeIpc)) -> SIGNAL_CTOR?) -> (() async -> Bool) {
        return _connectSignal.listen(cb)
    }
    
    override func afterShutdown() async {
        await super.afterShutdown()
        for inter_ipc in _connectedIpcSet {
            await inter_ipc.close()
        }
        
        _connectedIpcSet.removeAll()
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
