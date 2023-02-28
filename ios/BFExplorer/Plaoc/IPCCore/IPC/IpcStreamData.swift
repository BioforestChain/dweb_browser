//
//  IpcStreamData.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamData {

    let type = IPC_DATA_TYPE.STREAM_DATA
    private(set) var data: Any?
    private(set) var stream_id: String = ""
    
    init(stream_id: String, data: Any) {
        self.data = data
        self.stream_id = stream_id
    }
    
    static func fromBinary(ipc: Ipc, stream_id: String, data: [UInt8]) -> IpcStreamData {
        if ipc.supportMessagePack {
            return IpcStreamData(stream_id: stream_id, data: data)
        }
        return IpcStreamData(stream_id: stream_id, data: encoding.simpleDecoder(data: data, encoding: .base64))
    }
}

extension IpcStreamData: IpcMessage {
    
    init() {
        
    }
}
