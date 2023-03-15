//
//  MessagePortIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import Foundation
import Combine

class MessagePortIpc: Ipc {
    
    var port: MessagePort?
    private var cancellable: AnyCancellable?
    
    
    init(port: MessagePort, remote: MicroModuleInfo, role: IPC_ROLE = .CLIENT) {
        super.init()
        self.remote = remote
        self.role = role.rawValue
        self.port = port
        
        let ipc = self
        let callback = self.port?.onWebMessage { event in
            let message = jsonToIpcMessage.jsonToIpcMessage(data: event, ipc: ipc)
            if let message = message as? String {
                if message == "close" {
                    self.closeAction()
                } else if message == "ping" {
                    self.port?.postMessage(event: "pong")
                } else if message == "pong" {
                    print("PONG/\(ipc)")
                }
            } else if let ipcMessage = message as? IpcMessage {
                self.messageSignal?.emit((ipcMessage,ipc))
            } else {
                print("unknown message: \(message)")
            }
        }
        _ = onDestroy(cb: callback!)
    }
    
    convenience init(port: WebMessagePort, remote: MicroModuleInfo, role: IPC_ROLE) {
        
        self.init(port: MessagePort.from(port: port), remote: remote, role: role)
    }
    
    override func toString() -> String {
        return super.toString() + "@MessagePortIpc"
    }
    
    override func doPostMessage(data: IpcMessage) {
        guard let message = data.toJSONString() else { return }
        port?.postMessage(event: message)
    }
    
    override func doClose() {
        self.port?.postMessage(event: "close")
        self.port?.close()
    }
}
