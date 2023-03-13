//
//  MessagePortIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import Combine
import Vapor

class WebMessagePort: Hashable {
    let name: String
    let role: PortRole
    
    init(name: String, role: PortRole) {
        self.name = name
        self.role = role
    }
    
    func postMessage(_ message: String?) {
        if role == .port1 {
            NotificationCenter.default.post(name: Notification.Name(name + "_port2"), object: message)
        } else {
            NotificationCenter.default.post(name: Notification.Name(name + "_port1"), object: message)
        }
    }
    
    private var cancellable: AnyCancellable?
    
    func onMessage(_ callback: @escaping AsyncCallback<String, Any>) {
        var notiName = name
        
        if role == .port1 {
            notiName += "_port1"
        } else {
            notiName += "_port2"
        }
        
        let publisher = NotificationCenter.default.publisher(for: Notification.Name(notiName), object: nil)
        
        cancellable = publisher.sink { noti in
            if let message = noti.object as? String {
                Task {
                    _ = await callback(message)
                }
            }
        }
    }
    
    func close() {
        cancellable?.cancel()
    }
    
    deinit {
        cancellable?.cancel()
    }
    
    enum PortRole: String {
        case port1 = "port1"
        case port2 = "port2"
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(name)
        hasher.combine(role.rawValue)
    }
    
    static func ==(lhs: WebMessagePort, rhs: WebMessagePort) -> Bool {
        lhs.name == rhs.name && lhs.role.rawValue == rhs.role.rawValue
    }
}

class MessagePort {
    private static var wm: [WebMessagePort:MessagePort] = [:]
    static func from(port: WebMessagePort) -> MessagePort {
        if wm.keys.contains(where: { $0 == port }) {
            return wm[port]!
        } else {
            let messagePort = MessagePort(port: port)
            wm[port] = messagePort
            return messagePort
        }
    }
    
    private let port: WebMessagePort
    init(port: WebMessagePort) {
        self.port = port
    }
    
    private lazy var _messageSignal: Signal<String> = {
        let signal = Signal<String>()
        port.onMessage { message in
            await signal.emit(message)
        }
        
        return signal
    }()
    
    func onWebMessage(_ cb: @escaping AsyncCallback<String, Any>) -> AsyncVoidCallback<Bool> {
        return _messageSignal.listen(cb)
    }
    
    func postMessage(_ message: String) {
        port.postMessage(message)
    }
    
    func close() {
        port.close()
    }
}

class MessagePortIpc: Ipc {
    let port: MessagePort
    private let role_type: IPC_ROLE
    
    convenience init(port: WebMessagePort, remote: MicroModuleInfo, role_type: IPC_ROLE) {
        self.init(port: MessagePort.from(port: port), remote: remote, role_type: role_type)
    }
    
    init(port: MessagePort, remote: MicroModuleInfo, role_type: IPC_ROLE) {
        self.port = port
        self.role_type = role_type
        super.init()
        self.remote = remote
        self.role = role_type.rawValue
        
        let ipc = self
        let callback = port.onWebMessage { data in
            let message = jsonToIpcMessage(data: data, ipc: ipc)
            
            if let message = message as? IpcMessageString {
                if message.data == "close" {
                    await self.close()
                } else if message.data == "ping" {
                    self.port.postMessage("pong")
                } else if message.data == "pong" {
                    print("PONG/\(ipc)")
                } else {
                    fatalError("unknown message: \(message.data)")
                }
            } else if message != nil {
                await self._messageSignal.emit((message!, ipc))
            } else {
                fatalError("message is nil")
            }
            
            return nil
        }
    }
    
    override func toString() -> String {
        super.toString() + "@messagePortIpc"
    }
    
    override func _doPostMessage(data: IpcMessage) async {
        let message = JSONStringify(data)
        
        if message != nil {
            port.postMessage(message!)
        }
    }
    
    override func _doClose() async {
        port.postMessage("close")
        port.close()
    }
}
