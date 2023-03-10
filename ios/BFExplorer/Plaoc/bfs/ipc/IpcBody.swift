//
//  IpcBody.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

class IpcBody {
    /**
     * 缓存，这里不提供辅助函数，只是一个统一的存取地方，
     * 写入缓存者要自己维护缓存释放的逻辑
     */
    class CACHE {
        struct IpcBodyKey: Hashable {
            private let timestamp = Date().milliStamp
            let key: Any
            
            func hash(into hasher: inout Hasher) {
                hasher.combine(timestamp)
            }
            
            static func ==(lhs: IpcBodyKey, rhs: IpcBodyKey) -> Bool {
                lhs.timestamp == rhs.timestamp
            }
        }
        
        /**
         * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
         * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
         */
        static var raw_ipcBody_WMap: [IpcBodyKey:IpcBody] = [:]
        
        /**
         * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
         * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
         */
        static var metaId_receiverIpc_Map: [String:Ipc] = [:]
        
        /**
         * 每一个 metaBody 背后，都会有一个 IpcBodySender,
         * 这里主要是存储 流，因为它有明确的 open/close 生命周期
         */
        static var metaId_ipcBodySender_Map: [String:IpcBodySender] = [:]
    }
    
    
//    struct StreamData: Codable {
//        var stream: Data
//    }
    struct BodyHub {
        var text: String? = nil
        var stream: InputStream? = nil
        var u8a: Data? = nil
        var data: Any? = nil
    }
    
    lazy var bodyHub: BodyHub = BodyHub()
    var metaBody: MetaBody = MetaBody(type: .init(type: .stream_id), senderUid: 0, data: .init(string: nil, data: nil))
    
    var raw: Any {
        get {
            bodyHub.data!
        }
        set {
            bodyHub.data = newValue
        }
    }
    
    private lazy var _u8a: Data = {
        if bodyHub.u8a != nil {
            return bodyHub.u8a!
        } else if bodyHub.stream != nil {
            return Data(reading: bodyHub.stream!)
//            return bodyHub.stream!.stream
        } else if bodyHub.text != nil {
            return bodyHub.text!.fromBase64()!
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func u8a() -> Data {
        return _u8a
    }
    
    private lazy var _stream: InputStream = {
        if bodyHub.stream != nil {
//            return InputStream(data: bodyHub.stream!.stream)
            return bodyHub.stream!
        } else if bodyHub.u8a != nil {
            return InputStream(data: bodyHub.u8a!)
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func stream() -> InputStream {
        return _stream
    }
    
    private lazy var _text: String = {
        if bodyHub.text != nil {
            return bodyHub.text!
        } else if bodyHub.u8a != nil {
            return String(data: bodyHub.u8a!, encoding: .utf8)!
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func text() -> String {
        return _text
    }
}
