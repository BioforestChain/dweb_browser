//
//  NativeMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Network
import Vapor

class NativeMicroModule: MicroModule {

    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    typealias NativaCallback = (NativeIpc) -> Any
    
    private var connectedIpcSet = Set<Ipc>()
    
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    private let connectSignal = Signal<NativeIpc>()
    
    //TODO
//    private var  apiRouting: RoutingHttpHandler?
    
    init(mmid: String) {
        super.init()
        self.mmid = mmid
        _ = onConnect(cb: { clientIpc in
            clientIpc.onRequest { request,ipc in
                
            }
        })
    }
    
    override func _connect(from: MicroModule) -> NativeIpc? {
        let channel = NativeMessageChannel<IpcMessage, IpcMessage>()
        let nativeIpc = NativeIpc(port: channel.port1, remote: from, role: .SERVER)
        
        self.connectedIpcSet.insert(nativeIpc)
        _ = nativeIpc.onClose { _ in
            self.connectedIpcSet.remove(nativeIpc)
        }
        
        self.connectSignal.emit(nativeIpc)
        return NativeIpc(port: channel.port2, remote: self, role: .CLIENT)
    }
    
    func onConnect(cb: @escaping NativaCallback) -> OffListener {
        return self.connectSignal.listen(cb)
    }
    //在模块关停后，从自身构建的通讯通道都要关闭掉
    override func afterShutdown() {
        super.afterShutdown()
        for ipc in self.connectedIpcSet {
            ipc.closeAction()
        }
        self.connectedIpcSet.removeAll()
        
    }
    
    
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
    }
}
