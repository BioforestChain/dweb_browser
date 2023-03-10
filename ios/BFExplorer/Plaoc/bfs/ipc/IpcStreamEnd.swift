//
//  IpcStreamEnd.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

struct IpcStreamEnd {
    var type: IPC_MESSAGE_TYPE = .stream_end
    let stream_id: String
    
    init(stream_id: String) {
        self.stream_id = stream_id
    }
}

extension IpcStreamEnd: IpcMessage {}
