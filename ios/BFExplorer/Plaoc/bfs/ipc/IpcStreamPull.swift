//
//  IpcStreamPull.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

struct IpcStreamPull {
    var type: IPC_MESSAGE_TYPE = .stream_pull
    let stream_id: String
    var desiredSize: Int
    
    init(stream_id: String, desiredSize: Int = 1) {
        self.desiredSize = desiredSize
        self.stream_id = stream_id
    }
}

extension IpcStreamPull: IpcMessage {}
