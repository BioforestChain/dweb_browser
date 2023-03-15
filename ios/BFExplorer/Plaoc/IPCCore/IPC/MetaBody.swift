//
//  MetaBody.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/14.
//

import UIKit
import HandyJSON

class MetaBody: NSObject, HandyJSON {
    /**
         * 类型信息，包含了 编码信息 与 形态信息
         * 编码信息是对 data 的解释
         * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
         */
    var type: IPC_META_BODY_TYPE?
    var data: Any?
    var senderUid: Int = 0
    var streamId: String?
    var receiverUid: Int?
    /**
         * 唯一id，指代这个数据的句柄
         *
         * 需要使用这个值对应的数据进行缓存操作
         * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
         */
    var metaId: String {
        var bytes = [UInt8]()
        for _ in 0..<7 {
            let random = arc4random_uniform(255) - 128
            bytes.append(UInt8(random))
        }
        return bytes.toBase64Url()
    }
    
    var encoding: IPC_DATA_ENCODING {
        guard self.type != nil else { return .NONE }
        let encoding = self.type!.rawValue & 0b11111110
        return IPC_DATA_ENCODING(rawValue: encoding) ?? .NONE
    }
    
    var isInline: Bool {
        guard self.type != nil else { return false }
        let inline = self.type!.rawValue & 1
        return inline == 1
    }
    
    var isStream: Bool {
        guard self.type != nil else { return false }
        let inline = self.type!.rawValue & 1
        return inline == 0
    }
    
    var jsonAble: MetaBody? {
        if encoding == .BINARY {
            var dataString = ""
            if let content = data as? [UInt8] {
                dataString = content.toBase64()
            }
            return MetaBody.fromBase64(senderUid: senderUid, data: dataString, streamId: streamId, receiverUid: receiverUid)
        } else {
            return self
        }
    }
    
    override required init() {
        
        
    }
    
    init(type: IPC_META_BODY_TYPE, senderUid: Int, data: Any?, streamId: String?, receiverUid: Int?) {
        self.type = type
        self.senderUid = senderUid
        self.data = data
        self.streamId = streamId
        self.receiverUid = receiverUid
    }
    
    func or(TYPE: IPC_DATA_ENCODING) -> Int {
        guard self.type != nil else { return 0 }
        let result = self.type!.rawValue | TYPE.rawValue
        return Int(result)
    }
    
    static func fromText(senderUid: Int,
                         data: String,
                         streamId: String? = nil,
                         receiverUid: Int? = nil) -> MetaBody {
        
        let type = streamId == nil ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT
        return MetaBody(type: type, senderUid: senderUid, data: data, streamId: streamId, receiverUid: receiverUid)
    }
        
    static func fromBase64(senderUid: Int,
                         data: String,
                         streamId: String? = nil,
                         receiverUid: Int? = nil) -> MetaBody {
        
        let type = streamId == nil ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64
        return MetaBody(type: type, senderUid: senderUid, data: data, streamId: streamId, receiverUid: receiverUid)
    }
    
    static func fromBinary(senderUid: Int,
                         data: [UInt8],
                         streamId: String? = nil,
                         receiverUid: Int? = nil) -> MetaBody {
        
        let type = streamId == nil ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY
        return MetaBody(type: type, senderUid: senderUid, data: data, streamId: streamId, receiverUid: receiverUid)
    }
    
    static func fromBinary(senderIpc: Ipc,
                         data: [UInt8],
                         streamId: String? = nil,
                         receiverUid: Int? = nil) -> MetaBody {
        
        if senderIpc.supportBinary {
            return MetaBody.fromBinary(senderUid: senderIpc.uid, data: data, streamId: streamId, receiverUid: receiverUid)
        } else {
            return MetaBody.fromBase64(senderUid: senderIpc.uid, data: data.toBase64(), streamId: streamId, receiverUid: receiverUid)
        }
    }
}
