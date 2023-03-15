//
//  IpcStreamData.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

struct IpcStreamData {

    var type: IPC_MESSAGE_TYPE = .STREAM_DATA
    private(set) var data: Any?
    private(set) var stream_id: String = ""
    private var encoding: IPC_DATA_ENCODING = .NONE
    
    var binary: [UInt8]? {
        return binaryHelper.dataToBinary(data: data, encoding: encoding)
    }
    
    var text: String? {
        return binaryHelper.dataToText(data: data, encoding: encoding)
    }
    
    var jsonAble: IpcStreamData? {
        if encoding == .BINARY {
            return IpcStreamData.fromBase64(stream_id: stream_id, data: (data as? [UInt8]) ?? [])
        } else {
            return self
        }
    }
    
    init(stream_id: String, data: Any, encoding: IPC_DATA_ENCODING) {
        self.data = data
        self.stream_id = stream_id
        self.encoding = encoding
    }
    
    static func fromBinary(stream_id: String, data: [UInt8]) -> IpcStreamData {
        return IpcStreamData(stream_id: stream_id, data: data, encoding: .BINARY)
    }
    
    static func fromBase64(stream_id: String, data: [UInt8]) -> IpcStreamData {
        return IpcStreamData(stream_id: stream_id, data: data.toBase64(), encoding: .BASE64)
    }
    
    static func fromUtf8(stream_id: String, data: [UInt8]) -> IpcStreamData {
        return IpcStreamData(stream_id: stream_id, data: data.toUtf8(), encoding: .UTF8)
    }
    
    static func fromUtf8(stream_id: String, data: String) -> IpcStreamData {
        return IpcStreamData(stream_id: stream_id, data: data, encoding: .UTF8)
    }
}

extension IpcStreamData: IpcMessage {
    
    init() {
        
    }
}
