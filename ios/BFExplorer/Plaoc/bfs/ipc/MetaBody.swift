//
//  MetaBody.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/10.
//

import Foundation

struct MetaBody {
    struct IPC_META_BODY_TYPE: OptionSet, Codable {
        let rawValue: Int
        /** 流 */
        static let stream_id = IPC_META_BODY_TYPE(rawValue: 0)
        
        /** 内联数据 */
        static let inline = IPC_META_BODY_TYPE(rawValue: 1)
        
        /** 文本 json html 等 */
        static let stream_with_text = IPC_META_BODY_TYPE(rawValue: stream_id.rawValue | IPC_DATA_ENCODING.utf8.rawValue)
        /** 使用文本表示的二进制 */
        static let stream_with_base64 = IPC_META_BODY_TYPE(rawValue: stream_id.rawValue | IPC_DATA_ENCODING.base64.rawValue)
        /** 二进制 */
        static let stream_with_binary = IPC_META_BODY_TYPE(rawValue: stream_id.rawValue | IPC_DATA_ENCODING.binary.rawValue)
        /** 文本 json html 等 */
        static let inline_text = IPC_META_BODY_TYPE(rawValue: inline.rawValue | IPC_DATA_ENCODING.utf8.rawValue)
        /** 使用文本表示的二进制 */
        static let inline_base64 = IPC_META_BODY_TYPE(rawValue: inline.rawValue | IPC_DATA_ENCODING.base64.rawValue)
        /** 二进制 */
        static let inline_binary = IPC_META_BODY_TYPE(rawValue: inline.rawValue | IPC_DATA_ENCODING.binary.rawValue)
    }
    
    struct IpcMetaBodyType: Codable {
        var type: IPC_META_BODY_TYPE
        lazy var encoding: IPC_DATA_ENCODING = {
            let encoding = type.rawValue & 0b11111110
            return IPC_DATA_ENCODING.init(rawValue: encoding)
        }()
        
        lazy var isInline: Bool = {
            type.rawValue & 1 == 1
        }()
        
        lazy var isStream: Bool = {
            type.rawValue & 1 == 0
        }()
        
        init(type: IPC_META_BODY_TYPE) {
            self.type = type
        }
        
        init(from decoder: Decoder) throws {
            let value = try decoder.singleValueContainer()
            let rawValue = try value.decode(Int.self)
            type = IPC_META_BODY_TYPE(rawValue: rawValue)
        }
        
        func encode(to encoder: Encoder) throws {
            var container = encoder.singleValueContainer()
            try container.encode(type.rawValue)
        }
    }
    
    /**
     * 类型信息，包含了 编码信息 与 形态信息
     * 编码信息是对 data 的解释
     * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
     */
    var type: IpcMetaBodyType
    let senderUid: Int
    let data: IpcEvent.IpcEventData
    var streamId: String? = nil
    var receiverUid: Int? = nil
    
    /**
     * 唯一id，指代这个数据的句柄
     *
     * 需要使用这个值对应的数据进行缓存操作
     * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
     */
    var metaId: String = generateTokenBase64String(8)
    
    static func fromText(
        senderUid: Int,
        data: String,
        streamId: String? = nil,
        receiverUid: Int? = nil
    ) -> MetaBody {
        MetaBody(
            type: streamId == nil ? IpcMetaBodyType(type: .inline_text) : IpcMetaBodyType(type: .stream_with_text),
            senderUid: senderUid,
            data: .init(string: data, data: nil),
            streamId: streamId,
            receiverUid: receiverUid)
    }
    
    static func fromBase64(
        senderUid: Int,
        data: String,
        streamId: String? = nil,
        receiverUid: Int? = nil
    ) -> MetaBody {
        MetaBody(
            type: streamId == nil ? IpcMetaBodyType(type: .inline_base64) : IpcMetaBodyType(type: .stream_with_base64),
            senderUid: senderUid,
            data: .init(string: data, data: nil),
            streamId: streamId,
            receiverUid: receiverUid)
    }
    
    static func fromBinary(
        senderUid: Int,
        data: Data,
        streamId: String? = nil,
        receiverUid: Int? = nil
    ) -> MetaBody {
        MetaBody(
            type: streamId == nil ? IpcMetaBodyType(type: .inline_binary) : IpcMetaBodyType(type: .stream_with_binary),
            senderUid: senderUid,
            data: .init(string: nil, data: data),
            streamId: streamId,
            receiverUid: receiverUid)
    }
    
    static func fromBinary(
        senderIpc: Ipc,
        data: Data,
        streamId: String? = nil,
        receiverUid: Int? = nil
    ) -> MetaBody {
        senderIpc.suport_bianry
            ? fromBinary(
                senderUid: senderIpc.uid,
                data: data,
                streamId: streamId,
                receiverUid: receiverUid)
            : fromBase64(
                senderUid: senderIpc.uid,
                data: String(data: data, encoding: .utf8)!.base64String(),
                streamId: streamId,
                receiverUid: receiverUid)
    }
}

extension MetaBody: Codable {
    enum CodeKey: CodingKey {
        case type
        case senderUid
        case data
        case streamId
        case receiverUid
        case metaId
    }
    
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodeKey.self)
        
        type = try IpcMetaBodyType(type: IPC_META_BODY_TYPE(rawValue: values.decode(Int.self, forKey: .type)))
        senderUid = try values.decode(Int.self, forKey: .senderUid)
        streamId = try values.decode(String.self, forKey: .streamId)
        receiverUid = try values.decode(Int.self, forKey: .receiverUid)
        metaId = try values.decode(String.self, forKey: .metaId)
        data = try IpcEvent.IpcEventData(string: values.decode(String.self, forKey: .data), data: nil)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodeKey.self)
        
        try container.encode(type.type.rawValue, forKey: .type)
        try container.encode(senderUid, forKey: .senderUid)
        try container.encode(streamId, forKey: .streamId)
        try container.encode(receiverUid, forKey: .receiverUid)
        try container.encode(metaId, forKey: .metaId)
        
        if data.string != nil {
            try container.encode(data.string!, forKey: .data)
        } else if data.data != nil {
            try container.encode(String(data: data.data!, encoding: .utf8), forKey: .data)
        } else {
            fatalError("data value no found")
        }
    }
}
