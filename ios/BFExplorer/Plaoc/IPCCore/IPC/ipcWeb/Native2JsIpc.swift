//
//  Native2JsIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import UIKit

var ALL_MESSAGE_PORT_CACHE: [Int:MessagePort] = [:]
var all_ipc_id_acc = 1

func saveNative2JsIpcPort(port: WebMessagePort) {
    let port_id = all_ipc_id_acc
    all_ipc_id_acc += 1
    ALL_MESSAGE_PORT_CACHE[port_id] = MessagePort.from(port: port)
}

/**
 * Native2JsIpc 的远端是在 webView 中的，所以底层使用 WebMessagePort 与指通讯
 *
 * ### 原理
 * 连接发起方执行 `fetch('file://js.sys.dweb/create-ipc')` 后，
 * 由 js-worker 创建了 channel-port1/2，然后 js-process(native) 负责中转这个信道（在nwjs中，我们直接使用内存引用，在mobile中，我们需要拦截webRequest），并为其存下一个 id(number)。
 * 最终将这个 id 通过 fetch 返回值返回。
 *
 * 那么连接发起方就可以通过这个 id(number) 和 Native2JsIpc 构造器来实现与 js-worker 的直连
 */

class Native2JsIpc: MessagePortIpc {

    var port_id: Int = 0
    
    init(port_id: Int, remote: MicroModuleInfo, role: IPC_ROLE = .CLIENT) {
        
        let port = ALL_MESSAGE_PORT_CACHE[port_id]
        if port == nil {
            fatalError("no found port2(js-process) by id \(port_id)")
        }
        
        super.init(port: port!, remote: remote, role: role)
        
        self.port_id = port_id
        self.remote = remote
        self.role = role.rawValue
        
        _ = onClose { _ in
            ALL_MESSAGE_PORT_CACHE.removeValue(forKey: port_id)
        }
    }
   
}
