//
//  MessagePortIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import Foundation
import Combine

class MessagePortIpc: Ipc {
    
    var port: PassthroughSubject<String, Never>
    private var cancellable: AnyCancellable?
    
    init(port: PassthroughSubject<String, Never>, remote: MicroModule, role: IPC_ROLE) {
        self.port = port
        super.init()
        self.remote = remote
        self.role = role
        
        _ = port.sink { complete in
            
        } receiveValue: { value in
            let message = jsonToIpcMessage.jsonToIpcMessage(data: value, ipc: self) as? String
            switch message {
            case "close":
                self.closeAction()
            case "ping":
                self.closeAction()
            default:
                break
            }
        }
        
    }
    
    override func toString() -> String {
        return super.toString() + "@MessagePortIpc"
    }
}
