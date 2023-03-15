//
//  NativeIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import AsyncHTTPClient

class NativeIpc: Ipc {

    private var port: NativePort<IpcMessage, IpcMessage>?
    
    init(port: NativePort<IpcMessage, IpcMessage>, remote: MicroModule, role: IPC_ROLE) {
        super.init()
        self.port = port
        self.remote = remote
        self.role = role
        
        
        _ = port.onMessage { message in
//            var ipcMessage: IpcMessage?
//            if let request = message as? IpcRequest {
//                ipcMessage = IpcRequest.fromRequest(req_id: request.req_id, request: request.asRequest()!, ipc: self)
//            } else if let response = message as? IpcResponse {
//                if let res = IpcResponse.fromResponse(req_id: response.req_id, response: response.asResponse(), ipc: self) {
//                    ipcMessage = res
//                }
//            } else {
//                ipcMessage = message
//            }
            
            self.messageSignal?.emit((message,self))
//            return port.messageSignal.closure
        }
        Task {
            port.start()
        }
    }
    
    override func doPostMessage(data: IpcMessage) {
        port?.postMessage(msg: data)
    }
    
    override func doClose() {
        port?.close()
    }
}
