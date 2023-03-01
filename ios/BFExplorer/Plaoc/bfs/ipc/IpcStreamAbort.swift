//
//  IpcStreamAbort.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

struct IpcStreamAbort: Codable {
    var type: IPC_DATA_TYPE = .stream_abort
    let stream_id: String
    
    init(stream_id: String) {
        self.stream_id = stream_id
    }
}

extension IpcStreamAbort: IpcMessage {}
