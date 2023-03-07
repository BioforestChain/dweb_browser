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
        super.init(metaBody: metaBody, body: nil)
        
        let data = IpcBodyReceiver.rawDataToBody(metaBody: metaBody, ipc: ipc)
        if let data = data as? String {
            bodyHub = BodyHub(text: data)
            body = bodyHub
        } else if let data = data as? Data {
            bodyHub = BodyHub(u8a: data)
            body = bodyHub
        } else if let data = data as? StreamData {
            bodyHub = BodyHub(stream: data)
            body = bodyHub
        }
    }
    
    static func rawDataToBody(metaBody: MetaBody?, ipc: Ipc?) -> Any {
        if metaBody == nil || ipc == nil {
            return ""
        }

        var bodyEncoder: (Any) -> Any
        bodyEncoder = ((metaBody!.type.rawValue & IPC_RAW_BODY_TYPE.binary.rawValue) != 0)
            ? { data in return data as! Data }
            : ((metaBody!.type.rawValue & IPC_RAW_BODY_TYPE.base64.rawValue) != 0)
            ? { data in return (data as! String).to_b64_data()! }
            : ((metaBody!.type.rawValue & IPC_RAW_BODY_TYPE.text.rawValue) != 0)
            ? { data in return (data as! String).to_utf8_data()! }
            : { data in fatalError("invalid rawBody type: \(metaBody!.type)")}

        if !metaBody!.type.isEmpty && IPC_RAW_BODY_TYPE.stream_id.rawValue != 0 {
            let stream_id = metaBody!.data.string!
            let stream = ReadableStream(onStart: { controller in
                _ = ipc!.onMessage { (message, ipc) in
                    if let message = message as? IpcStreamData, message.stream_id == stream_id {
    //                    let str = String(decoding: message.data, as: .)
                        let str = String(decoding: bodyEncoder(message.data) as! Data, as: UTF8.self)

                        controller.enqueue(ByteBuffer.init(string: str))
                    } else if let message = message as? IpcStreamEnd, message.stream_id == stream_id {
                        return .OFF
                    }

                    return nil
                }
            }, onPull: { (desiredSize, controller) in
                await ipc!.postMessage(message: IpcStreamPull(stream_id: stream_id, desiredSize: desiredSize))
            })
            return IpcBodyReceiver.StreamData(stream: Data(reading: stream))
        } else if (metaBody!.type.rawValue & IPC_RAW_BODY_TYPE.text.rawValue) != 0 {
            return metaBody!.data.string!
        } else {
            return bodyEncoder(metaBody!.data.data!)
        }
    }
}
