//
//  IpcEvent.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/6.
//

import UIKit

struct IpcEvent {

    var type: IPC_MESSAGE_TYPE = .STREAM_EVENT
    private(set) var name: String?
    private(set) var data: Any?
    private var encoding: IPC_DATA_ENCODING = .NONE
    
    var binary: [UInt8]? {
        return binaryHelper.dataToBinary(data: data, encoding: encoding)
    }
    
    var text: String? {
        return binaryHelper.dataToText(data: data, encoding: encoding)
    }
    
    var jsonAble: IpcEvent? {
        if encoding == .BINARY {
            return IpcEvent.fromBase64(name: name ?? "", data: (data as? [UInt8]) ?? [])
        } else {
            return self
        }
    }
    
    init(name: String, data: Any, encoding: IPC_DATA_ENCODING) {
        self.data = data
        self.name = name
        self.encoding = encoding
    }
    
    static func fromBinary(name: String, data: [UInt8]) -> IpcEvent {
        return IpcEvent(name: name, data: data, encoding: .BINARY)
    }
    
    static func fromBase64(name: String, data: [UInt8]) -> IpcEvent {
        return IpcEvent(name: name, data: data.toBase64(), encoding: .BASE64)
    }
    
    static func fromUtf8(name: String, data: [UInt8]) -> IpcEvent {
        return IpcEvent(name: name, data: data.toUtf8(), encoding: .UTF8)
    }
    
    static func fromUtf8(name: String, data: String) -> IpcEvent {
        return IpcEvent(name: name, data: data, encoding: .UTF8)
    }
}

extension IpcEvent: IpcMessage {
    
    init() {
        
    }
}
