//
//  MessagePort.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import UIKit

typealias MessagePortCallback = (String) -> Void

class MessagePort {

    static private var wm: [WebMessagePort: MessagePort] = [:]
    private var port: WebMessagePort
    
    init(port: WebMessagePort) {
        self.port = port
    }
    
    static func from(port: WebMessagePort) -> MessagePort {
        var messsagePort = wm[port]
        if messsagePort == nil {
            messsagePort = MessagePort(port: port)
            wm[port] = messsagePort
        }
        return wm[port]!
    }
    
    lazy var messageSignal: Signal<String> = {
        let signal = Signal<String>()
        port.onMessage { message in
            signal.emit(message)
        }
        return signal
    }()
    
    func onWebMessage(cb:  @escaping MessagePortCallback) -> OffListener {
        return messageSignal.listen(cb)
    }
    
    func postMessage(event: String) {
        port.postMessage(event)
    }
    
    func close() {
        port.close()
    }
}
