//
//  IpcEvent.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/6.
//

import UIKit

struct IpcEvent {

    var type: IPC_DATA_TYPE = .STREAM_EVENT
    private(set) var name: String?
    private(set) var data: Any?
    
    init(name: String, data: Any) {
        
        self.name = name
        self.data = data
    }
}

extension IpcEvent: IpcMessage {
    
    init() {
        
    }
}
