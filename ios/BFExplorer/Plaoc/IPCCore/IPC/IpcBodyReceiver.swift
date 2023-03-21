//
//  IpcBodyReceiver.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit

/**
 * metaBody 可能会被多次转发，
 * 但只有第一次得到这个 metaBody 的 ipc 才是它真正意义上的 Receiver
 */

class IpcBodyReceiver: IpcBody {
    
    init(metaBody: MetaBody?, ipc: Ipc?) {
        super.init()
        self.metaBody = metaBody
        self.ipc = ipc
        self.bodyHub = hub
        
        // 将第一次得到这个metaBody的 ipc 保存起来，这个ipc将用于接收
        if metaBody?.isStream ?? false {
            guard metaBody != nil else { return }
            let metaIpc = metaId_receiverIpc_Map[metaBody!.metaId]
            if metaIpc == nil {
                _ = ipc?.onClose(cb: { _ in
                    metaId_receiverIpc_Map.removeValue(forKey: metaBody!.metaId)
                })
                metaBody?.receiverUid = ipc?.uid
                metaId_receiverIpc_Map[metaBody!.metaId] = ipc
            }
        }
    }
    
    lazy private var hub: BodyHub = {
        var body = BodyHub()
//        guard metaBody != nil else { return body }
//        var data: Any?
//        if metaBody!.isStream {
//            guard let ipc = metaId_receiverIpc_Map[metaBody!.metaId] else {
//                print("no found ipc by metaId:\(metaBody!.metaId)")
//                return
//            }
//            metaToStream(metaBody: metaBody, ipc: ipc)
//        } else {
//            if metaBody!.encoding == .UTF8 {
//                if let dataStr = metaBody?.data as? String {
//                    data = dataStr
//                }
//            } else if metaBody!.encoding == .BINARY {
//                if let bytes = metaBody?.data as? [UInt8] {
//                    data = bytes
//                }
//            } else if metaBody!.encoding == .BASE64 {
//                if let dataStr = metaBody?.data as? String {
//                    data = dataStr.fromBase64()
//                }
//            } else {
//                print("invalid metaBody type:\(metaBody!.type)")
//                return
//            }
//        }
//        body.data = data
//        if let dataStr = data as? String {
//            body.text = dataStr
//        } else if let bytes = data as? [UInt8] {
//            body.u8a = bytes
//        } else if let stream = data as? InputStream {
//            body.stream = stream
//        }
        return body
    }()
    
    func from(metaBody: MetaBody, ipc: Ipc) -> IpcBody {
        return metaId_ipcBodySender_Map[metaBody.metaId] ?? IpcBodyReceiver(metaBody: metaBody, ipc: ipc)
    }
    
    func metaToStream(metaBody: MetaBody?, ipc: Ipc?) -> InputStream {
        
        let stream_id = metaBody?.streamId ?? ""
        
        let stream = ReadableStream(cid: "receiver-\(stream_id)") { controller in
            var firstData: [UInt8]?
            if metaBody?.encoding == .UTF8 {
                firstData = (metaBody?.data as? String)?.fromUtf8()
            } else if metaBody?.encoding == .BINARY {
                firstData = metaBody?.data as? [UInt8]
            } else if metaBody?.encoding == .BASE64 {
                firstData = (metaBody?.data as? String)?.fromBase64()
            }
            if firstData != nil {
                controller.enqueue(Data(firstData!))
            }
            _ = ipc?.onMessage(cb: { message,_ in
                if let request = message as? IpcStreamData, request.stream_id == stream_id {
                    controller.enqueue(Data(request.binary ?? []))
                } else if let request = message as? IpcStreamEnd, request.stream_id == stream_id {
                    controller.close()
                }
            })
        } onPull: { desiredSize, controller in
            ipc?.postMessage(message: IpcStreamPull(stream_id: stream_id, desiredSize: 1))
        }

        return stream
    }
}
