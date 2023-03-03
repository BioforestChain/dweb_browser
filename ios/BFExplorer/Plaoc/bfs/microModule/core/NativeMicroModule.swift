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

class NativeMicroModule: MicroModule {
    override init() {
        super.init()
        mmid = ".sys.dweb"
        
        let app = HttpServer.app
        let hookRequestMiddleware = HookRequestMiddleware()
        app.middleware.use(hookRequestMiddleware, at: .end)
        let middlewares = app.middleware.resolve()
        
        
        _ = onConnect { clientIpc in
            _ = clientIpc.onRequest { message in
                hookRequestMiddleware.hookRequest(ipcRequest: message.0, clientIpc: clientIpc)
                return nil
            }
            return nil
        }
    }
    
    struct HookRequestMiddleware: AsyncMiddleware {
        var channel = PassthroughSubject<(IpcRequest, Ipc), Never>()
        
        func hookRequest(ipcRequest: IpcRequest, clientIpc: Ipc) {
            channel.send((ipcRequest, clientIpc))
        }

        func respond(to request: Vapor.Request, chainingTo next: Vapor.AsyncResponder) async throws -> Vapor.Response {
            var resPo = PromiseOut<Vapor.Response>()
            for await (ipcRequest, clientIpc) in channel.values {
                let req = ipcRequest.toRequest()
                
                if request.route?.path.string == req.route?.path.string {
                    req.ipc = clientIpc
                    let response = try await next.respond(to: req)
                    let ipcResMessage = IpcResponse.fromResponse(req_id: ipcRequest.req_id, response: response, ipc: clientIpc).ipcResMessage
                    clientIpc.postMessage(message: ipcResMessage)
                    resPo.resolve(response)
                } else {
                    resPo.resolve(try await next.respond(to: request))
                }
            }

            return await resPo.waitPromise()
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
        _connectSignal.emit((nativeIpc))
        return NativeIpc(port: channel.port2, remote: self, role: .client)
    }
    
    internal var _connectSignal = Signal<(NativeIpc)>()
    
    internal func onConnect(cb: @escaping ((NativeIpc)) -> SIGNAL_CTOR?) -> (() -> Bool) {
        return _connectSignal.listen(cb)
    }
    
    override func afterShutdown() async {
        await super.afterShutdown()
        for inter_ipc in _connectedIpcSet {
            await inter_ipc.close()
        }
        
        _connectedIpcSet.removeAll()
    }
}

extension Vapor.Request {
    struct createIpc {
        static var _clientIpc: Ipc? = nil
    }
    
    var ipc: Ipc? {
        get {
            return createIpc._clientIpc
        }
        set {
            createIpc._clientIpc = newValue
        }
    }
}

extension RoutesBuilder {
    func on<Response>(
        _ method: HTTPMethod,
        _ path: [PathComponent],
        body: HTTPBodyStreamStrategy = .collect,
        use closure: @escaping (Request, Ipc?) async throws -> Response
    ) -> Route
        where Response: AsyncResponseEncodable
    {
        let responder = AsyncBasicResponder { request  in
            if case .collect(let max) = body, request.body.data == nil {
                _ = try await request.body.collect(max: max?.value ?? request.application.routes.defaultMaxBodySize.value).get()
                
            }
            return try await closure(request, request.ipc).encodeResponse(for: request)
        }
        let route = Route(
            method: method,
            path: path,
            responder: responder,
            requestType: Request.self,
            responseType: Response.self
        )
        self.add(route)
        return route
    }
}
