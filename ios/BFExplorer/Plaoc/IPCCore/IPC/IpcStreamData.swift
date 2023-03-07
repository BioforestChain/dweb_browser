//
//  IpcStreamData.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamData {

    var type: IPC_DATA_TYPE = .STREAM_MESSAGE
    private(set) var data: Any?
    private(set) var stream_id: String = ""
    private var encoding: IPC_DATA_ENCODING?
    
    init(stream_id: String, data: Any, encoding: IPC_DATA_ENCODING) {
        self.data = data
        self.stream_id = stream_id
        self.encoding = encoding
    }
    
    static func fromBinary(ipc: Ipc, stream_id: String, data: [UInt8]) -> IpcStreamData {
        if ipc.support_binary {
            return asBinary(stream_id: stream_id, data: data)
        }
        return asBase64(stream_id: stream_id, data: data)
    }
    
    static func asBinary(stream_id: String, data: [UInt8]) -> IpcStreamData {
        return IpcStreamData(stream_id: stream_id, data: data, encoding: .BINARY)
    }
    
    static func asBase64(stream_id: String, data: [UInt8]) -> IpcStreamData {
        let datas = Data(bytes: data, count: data.count)
        return IpcStreamData(stream_id: stream_id, data: datas.base64EncodedString(), encoding: .BASE64)
    }
    
    static func asUtf8(stream_id: String, data: [UInt8]) -> IpcStreamData {
        let datas = Data(bytes: data, count: data.count)
        return IpcStreamData(stream_id: stream_id, data: String(data: datas, encoding: .utf8) ?? "", encoding: .UTF8)
        
    }
}

extension IpcStreamData: IpcMessage {
    
    init() {
        
    }
}
