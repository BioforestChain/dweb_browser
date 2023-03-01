//
//  Native2JsIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import Combine

var ALL_IPC_CACHE: [Int:String] = [:]
var all_ipc_id_acc = 0

func saveNative2JsIpcPort(port: String) {
    let port_id = all_ipc_id_acc++
    ALL_IPC_CACHE[port_id] = port
}

/// Native2JsIpc通常是port2，所以type设置为port2
class Native2JsIpc: MessagePortIpc {
    init(port_id: Int, remote: MicroModule, role: IPC_ROLE = .client) {
        let port = ALL_IPC_CACHE[port_id]
        
        if port == nil {
            fatalError("no found port js-process by id \(port_id)")
        }
        
        super.init(port: port!, remote: remote, role: role, type: .port2)
    }
}
