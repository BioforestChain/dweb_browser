//
//  jsonToIpcMessage.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit
import HandyJSON

class jsonToIpcMessage: NSObject {

    
    static func jsonToIpcMessage(data: String, ipc: Ipc) -> Any? {
        if (data == "close" || data == "ping" || data == "pong") {
            return data
        }
        
        let baseModel = JSONDeserializer<BaseStruct>.deserializeFrom(json: data)
        if baseModel?.type == .REQUEST {
            if let reqMessage = JSONDeserializer<IpcReqMessage>.deserializeFrom(json: data) {
                let header = IpcHeaders()
                for (key,value) in reqMessage.headers {
                    header.set(key: key, value: value)
                }
                return IpcRequest(req_id: reqMessage.req_id, method: reqMessage.method, urlString: reqMessage.urlString, headers: header, body: IpcBodyReceiver(metaBody: reqMessage.metaBody, ipc: ipc), ipc: ipc)
            }
        }
        
        if baseModel?.type == .RESPONSE {
            if let reqMessage = JSONDeserializer<IpcResMessage>.deserializeFrom(json: data) {
                let header = IpcHeaders()
                let headerDict = reqMessage.headers.headerDict
                for (key,value) in headerDict  {
                    header.set(key: key, value: value)
                }
                return IpcResponse(req_id: reqMessage.req_id, statusCode: reqMessage.statusCode, headers: header, body: IpcBodyReceiver(metaBody: reqMessage.metaBody, ipc: ipc), ipc: ipc)
            }
        }
        
        if baseModel?.type == .STREAM_DATA {
            return JSONDeserializer<IpcStreamData>.deserializeFrom(json: data)
        }
        
        if baseModel?.type == .STREAM_PULL {
            return JSONDeserializer<IpcStreamPull>.deserializeFrom(json: data)
        }
        if baseModel?.type == .STREAM_END {
            return JSONDeserializer<IpcStreamEnd>.deserializeFrom(json: data)
        }
        if baseModel?.type == .STREAM_ABORT {
            return JSONDeserializer<IpcStreamAbort>.deserializeFrom(json: data)
        }
        if baseModel?.type == .STREAM_EVENT {
            return JSONDeserializer<IpcEvent>.deserializeFrom(json: data)
        }
        
        return data
    }
}
