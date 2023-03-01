//
//  MessagePortIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import Combine
import Vapor

class MessagePortIpc: Ipc {
    private var type: MessagePortIpcType
    private var port1: String
    private var port2: String
    private var cancellable: AnyCancellable?
    
    enum MessagePortIpcType {
        case port1
        case port2
    }
    
    init(port: String, remote: MicroModule, role: IPC_ROLE, type: MessagePortIpcType) {
        self.type = type
        if type == .port1 {
            port1 = port + "_sender"
            port2 = port + "_receiver"
        } else {
            port2 = port + "_sender"
            port1 = port + "_receiver"
        }
        
        super.init()
        self.remote = remote
        self.role = role
        self.support_message_pack = true
        self.support_protobuf = false

        let ipc = self
        let publisher = NotificationCenter.default.publisher(for: Notification.Name(port2), object: nil)
        cancellable = publisher.sink { noti in
            if let message = noti.object as? String {
                if message == "close" {
                    Task {
                        await self.close()
                    }
                } else if message == "ping" {
                    NotificationCenter.default.post(name: Notification.Name(self.port1), object: "pong")
                } else if message == "pong" {
                    print("PONG/\(ipc)")
                }
            } else if let message = noti.object as? IpcMessage {
                self._messageSignal.emit((message, ipc))
            }
        }
    }
    
    override func _doPostMessage(data: IpcMessage) {
        if let message = data as? IpcReqMessage {
            NotificationCenter.default.post(name: Notification.Name(port1), object: message)
        } else if let message = data as? IpcResMessage {
            NotificationCenter.default.post(name: Notification.Name(port1), object: message)
        } else {
            NotificationCenter.default.post(name: Notification.Name(port1), object: data as! IpcMessageData)
        }
    }
    
    override func _doClose() async {
        NotificationCenter.default.post(name: Notification.Name(port1), object: "close")
        cancellable?.cancel()
    }
}
