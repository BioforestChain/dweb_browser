//
//  IpcStreamPull.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamPull {

    var type: IPC_DATA_TYPE = .STREAM_PULL
    private(set) var desiredSize: Int = 0
    private(set) var stream_id: String?
    
    init(stream_id: String, desiredSize: Int = 1) {
        
        self.stream_id = stream_id
        self.desiredSize = desiredSize
    }

}

extension IpcStreamPull: IpcMessage {
    
    init() {
        
    }
}
