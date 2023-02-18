//
//  rawDataToBody.swift
//  IPC
//
//  Created by ui03 on 2023/2/14.
//

import UIKit

class rawDataToBody {

    static func rawDataToBodyResult(rawBody: RawData, ipc: Ipc?) -> Any? {
        
        var body: Any?
        let raw_body_type = rawBody.type
        var emptyResult: (() -> Bool)!
        if raw_body_type == .STREAM_ID {
            guard ipc != nil else {
                print("miss ipc when ipc-response has stream-body")
                return nil
            }
            let stream_ipc = ipc!
            let stream_id = rawBody.result as? String
            //TODO
            var stream: InputStream?
            body = stream
            stream?.open()
            
            defer {
                stream?.close()
            }
            
            let funct = ipc?.onMessage { (message,ipc) in
                if let message = message as? IpcStreamData, message.stream_id == stream_id  {
                    
                    var resultData: [UInt8]?
                    if message.data is String {
                        let dataString = message.data as? String ?? ""
                        resultData = rawDataToBody.bodyEncoder(type: .TEXT, result: dataString)
                    } else {
                        resultData = message.data as? [UInt8]
                    }
                    if resultData != nil {
                        let data = Data(bytes: resultData!, count: resultData!.count)
                        stream = InputStream(data: data)
                    }
                } else if let message = message as? IpcStreamEnd, message.stream_id == stream_id  {
                    //TODO Signal.off
                }
                return emptyResult
            }()
        } else {
            body = raw_body_type == .TEXT ? rawBody.result : rawDataToBody.bodyEncoder(type: raw_body_type!, result: rawBody.result)
        }
        return body
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
