//
//  IpcStreamData.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

struct IpcStreamData: Codable {
    var type: IPC_DATA_TYPE = .stream_data
    let stream_id: String
    let data: S_RawData
    
    init(stream_id: String, data: S_RawData) {
        self.stream_id = stream_id
        self.data = data
    }
    
    static func fromBinary(ipc: Ipc, stream_id: String, data: Data) -> IpcStreamData {
        if ipc.suport_bianry {
            return IpcStreamData(stream_id: stream_id, data: S_RawData(data: data))
        }
        
        return IpcStreamData(stream_id: stream_id, data: S_RawData(string:data.base64EncodedString()))
    }
}

extension IpcStreamData: IpcMessage {}
