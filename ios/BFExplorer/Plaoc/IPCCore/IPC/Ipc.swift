//
//  Ipc.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

var ipc_uid_acc = 0

class Ipc: NSObject {

    var support_message_pack: Bool?
    var uid: Int {
        ipc_uid_acc = ipc_uid_acc + 1
        return ipc_uid_acc
    }
    var remote: MicroModule?
    var role: IPC_ROLE?
    
    private var closed: Bool = false
    
    private var ipcMessage: OnIpcMessage?
    
//    var messageSignal = Signal.createSignal(callback: ipcMessage)
    
    
    func postMessage(message: NSObject) {
        guard !closed else { return }
        doPostMessage(data: message)
    }
    
    func doPostMessage(data: NSObject) { }
    
    func getOnRequestListener() {
//        let signal = Signal.createSignal()
        
    }
}
