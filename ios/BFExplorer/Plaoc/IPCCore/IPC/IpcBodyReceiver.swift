//
//  IpcBodyReceiver.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit
import SwiftUI
import Vapor

class IpcBodyReceiver: IpcBody {
    
    init(metaBody: MetaBody?, ipc: Ipc?) {
        super.init()
        self.metaBody = metaBody
        self.ipc = ipc
        self.bodyHub = hub
    }
    
    lazy private var hub: Body = {
        var body = Body()
        let data = IpcBodyReceiver.rawDataToBody(metaBody: self.metaBody, ipc: self.ipc)
        body.data = data
        if let str = data as? String {
            body.text = str
        } else if let bytes = data as? [UInt8] {
            body.u8a = bytes
        } else if let stream = data as? InputStream {
            body.stream = stream
        }
        return body
    }()
    
    static func rawDataToBody(metaBody: MetaBody?, ipc: Ipc?) -> Any {
        
        if metaBody == nil || ipc == nil {
            return ""
        }
        
        if metaBody?.type == .STREAM_ID {
            let stream_id = metaBody?.data as? String
            let stream = ReadableStream().startLoad { controller in
                _ = ipc?.onMessage({ (message, _) in
               
                    if let request = message as? IpcStreamData, request.stream_id == stream_id {
                        controller.enqueue(byteArray: self.bodyEncoder(type: IPC_RAW_BODY_TYPE(rawValue: metaBody!.type!.rawValue) ?? .TEXT, result: request.data) ?? [])
                    } else if let request = message as? IpcStreamEnd, request.stream_id == stream_id {
                        controller.close()
                        if ipc!.messageSignal != nil, ipc!.messageSignal?.closure != nil {
                            ipc!.messageSignal?.removeCallback(cb: ipc!.messageSignal!.closure!)
                        }
                    }
                    return ipc?.messageSignal?.closure
                })
            } onPull: { desiredSize, controller in
                ipc!.postMessage(message: IpcStreamPull(stream_id: stream_id!, desiredSize: desiredSize))
            }
            return stream
        } else if metaBody?.type == .TEXT {
            return metaBody?.data as? String
        } else {
            return bodyEncoder(type: IPC_RAW_BODY_TYPE(rawValue: metaBody!.type!.rawValue) ?? .TEXT, result: metaBody!.data)
        }
        
    }
    
    static func bodyEncoder(type: IPC_RAW_BODY_TYPE, result: Any) -> [UInt8]? {
        if type == .BINARY {
            return result as? [UInt8]
        } else if type == .BASE64 {
            return encoding.simpleEncoder(data: result as? String ?? "", encoding: .base64)
        } else if type == .TEXT {
            return encoding.simpleEncoder(data: result as? String ?? "", encoding: .utf8)
        } else {
            return nil
        }
    }
    
}
