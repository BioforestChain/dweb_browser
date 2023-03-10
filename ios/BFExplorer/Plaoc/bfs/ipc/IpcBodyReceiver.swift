//
//  IpcBodyReceiver.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation
import Vapor

class IpcBodyReceiver: IpcBody {
    private let ipc: Ipc
    
    init(metaBody: MetaBody, ipc: Ipc) {
        self.ipc = ipc
        super.init()
        self.metaBody = metaBody
        
        if self.metaBody.type.isStream {
            let cacheIpc = CACHE.metaId_receiverIpc_Map[self.metaBody.metaId]
            
            if cacheIpc == nil {
                _ = ipc.onClose {
                    CACHE.metaId_receiverIpc_Map.removeValue(forKey: self.metaBody.metaId)
                    return .OFF
                }
                self.metaBody.receiverUid = ipc.uid
                
                CACHE.metaId_receiverIpc_Map[self.metaBody.metaId] = ipc
            }
        }
    }
    
    private lazy var _bodyHub: IpcBody.BodyHub = {
        var bodyHub = BodyHub()

        if self.metaBody.type.isStream {
            bodyHub.stream = IpcBodyReceiver.metaToStream(metaBody: self.metaBody, ipc: self.ipc)
            bodyHub.data = bodyHub.stream
        } else {
            switch self.metaBody.type.encoding {
            case .utf8:
                bodyHub.text = self.metaBody.data.string!
                bodyHub.data = bodyHub.text
            case .binary:
                bodyHub.u8a = self.metaBody.data.data!
                bodyHub.data = bodyHub.u8a
            case .base64:
                bodyHub.u8a = self.metaBody.data.string!.fromBase64()!
                bodyHub.data = bodyHub.u8a
            default:
                fatalError("invalid metaBody type: \(self.metaBody.type)")
            }
        }
        
//        switch data {
//        case is String:
//            bodyHub.text = data as! String
//        case is Data:
//            bodyHub.u8a = data as! Data
//        case is InputStream:
//            bodyHub.stream = data as! InputStream
//        default:
//            fatalError("invalid data type: \(data)")
//        }
        return bodyHub
    }()
    override var bodyHub: IpcBody.BodyHub {
        get {
            _bodyHub
        }
        set {
            _bodyHub = newValue
        }
    }
    
    static func from(metaBody: MetaBody, ipc: Ipc) -> IpcBody {
        return CACHE.metaId_ipcBodySender_Map[metaBody.metaId] ?? IpcBodyReceiver(metaBody: metaBody, ipc: ipc)
    }
    
    static func metaToStream(metaBody: MetaBody, ipc: Ipc) -> InputStream {
        var metaBody = metaBody
        let stream_id = metaBody.streamId!
        let stream = ReadableStream(onStart: { controller in
            var data: Data?
            switch metaBody.type.encoding {
            case .utf8:
                data = metaBody.data.string!.fromUtf8()
            case .binary:
                data = metaBody.data.data!
            case .base64:
                data = metaBody.data.string!.fromBase64()
            default:
                data = nil
            }
            
            _ = ipc.onMessage { (message, _) in
                if let message = message as? IpcStreamData, message.stream_id == stream_id {
//                    if stream_id == "rs-0" {
//
//                    }
                } else if let message = message as? IpcStreamEnd, message.stream_id == stream_id {
                    
                    
                    return SIGNAL_CTOR.OFF
                }
                
                return nil
            }
        }, onPull: { (desiredSize, controller) in
            print("receiver/postPullMessage/\(ipc)/")
            await ipc.postMessage(message: IpcStreamPull(stream_id: stream_id, desiredSize: 1))
        })
        
        return stream
    }
}
