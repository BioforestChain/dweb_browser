//
//  IpcStreamAbort.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamAbort {

    var type: IPC_DATA_TYPE = .STREAM_ABORT
    private(set) var stream_id: String?
    
    init(stream_id: String) {
        self.stream_id = stream_id
    }
}

extension IpcStreamAbort: IpcMessage {
    
    init() {
        
    }
}
