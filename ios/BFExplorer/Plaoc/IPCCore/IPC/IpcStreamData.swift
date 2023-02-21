//
//  IpcStreamData.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcStreamData: NSObject {

    let type = IPC_DATA_TYPE.STREAM_DATA
    private(set) var data: Any?
    private(set) var stream_id: String = ""
    
    init(stream_id: String, data: Any) {
        super.init()
        self.data = data
        self.stream_id = stream_id
    }
    
    static func fromBinary(ipc: Ipc, stream_id: String, data: [UInt8]) -> IpcStreamData {
        if ipc.support_message_pack ?? false {
            return IpcStreamData(stream_id: stream_id, data: data)
        }
        return IpcStreamData(stream_id: stream_id, data: encoding.simpleDecoder(data: data, encoding: .base64))
    }
}
