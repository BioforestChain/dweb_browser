//
//  IpcStreamEnd.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamEnd {

    let type = IPC_DATA_TYPE.STREAM_END
    private(set) var stream_id: String = ""
    
    init(stream_id: String) {
        
        self.stream_id = stream_id
    }
}

extension IpcStreamEnd: IpcMessage {
    init() {
        
    }
}
