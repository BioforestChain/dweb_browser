//
//  IpcEvent.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/10.
//

import Foundation

struct IpcEvent {
    var type: IPC_MESSAGE_TYPE = .event
    
    let name: String
    let data: IpcEventData /* String or Data */
    let encoding: IPC_DATA_ENCODING
    
    struct IpcEventData: Codable {
        let string: String?
        let data: Data?
    }
    
    var binary: Data {
        get {
            dataToBinary(data: data, encoding: encoding)
        }
    }
    
    var text: String {
        get {
            dataToText(data: data, encoding: encoding)
        }
    }
}

extension IpcEvent: IpcMessage {
    enum CodeKey: CodingKey {
        case type
        case name
        case data
        case encoding
    }
    
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodeKey.self)
        type = try values.decode(IPC_MESSAGE_TYPE.self, forKey: .type)
        name = try values.decode(String.self, forKey: .name)
        encoding = try values.decode(IPC_DATA_ENCODING.self, forKey: .encoding)
        
        let value = try values.decode(String.self, forKey: .data)
//        switch encoding {
//        case .binary:
//            data = IpcEventData(string: nil, data: value.fromUtf8())
//        case .base64, .utf8:
//            data = IpcEventData(string: value, data: nil)
//        default:
//            fatalError("encoding type unknown, can not deserialize data")
//        }
        data = IpcEventData(string: value, data: nil)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodeKey.self)
        try container.encode(type, forKey: .type)
        try container.encode(name, forKey: .name)
        try container.encode(encoding, forKey: .encoding)
        
//        switch encoding {
//        case .binary:
//            try container.encode(String(data: data.data!, encoding: .utf8), forKey: .data)
//        case .base64, .utf8:
//            try container.encode(data.string!, forKey: .data)
//        default:
//            fatalError("encoding type unknown, can not serialize data")
//        }
        if data.string != nil {
            try container.encode(data.string!, forKey: .data)
        } else if data.data != nil {
            try container.encode(String(data: data.data!, encoding: .utf8), forKey: .data)
        } else {
            fatalError("data value no found")
        }
    }
}
