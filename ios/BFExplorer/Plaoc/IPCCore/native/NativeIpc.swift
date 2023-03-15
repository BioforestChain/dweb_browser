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
    
    init(port: NativePort<IpcMessage, IpcMessage>, remote: MicroModuleInfo, role: IPC_ROLE) {
        super.init()
        self.port = port
        self.remote = remote
        self.role = role.rawValue
        
        self.supportRaw = true
        self.supportBinary = true
        
        _ = port.onMessage { message in
            
            self.messageSignal?.emit((message,self))
        }
        Task {
            port.start()
        }
    }
    
    override func toString() -> String {
        return super.toString() + "@NativeIpc"
    }
    
    override func doPostMessage(data: IpcMessage) {
        port?.postMessage(msg: data)
    }
    
    override func doClose() {
        port?.close()
    }
}
