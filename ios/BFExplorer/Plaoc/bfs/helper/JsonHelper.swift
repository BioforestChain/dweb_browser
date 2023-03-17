//
//  JsonHelper.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation

/// 序列化
func JSONStringify<T: Codable>(_ data: T) -> String? {
    do {
        let jsonData = try JSONEncoder().encode(data)
        return String(data: jsonData, encoding: .utf8)
    } catch {
        fatalError("data JSONStringify error: \(data)")
    }
}

/// 反序列化
func JSONParse<T: Codable>(_ str: String, of type: T.Type) -> T {
    do {
        let jsonData = str.utf8Data()
        
        if jsonData == nil {
            fatalError("data JSONParse error: \(str)")
        }
        
        return try JSONDecoder().decode(type, from: jsonData!)
    } catch {
        fatalError("data JSONParse error: \(error.localizedDescription)")
    }
}

func jsonToIpcMessage(data: String, ipc: Ipc) -> IpcMessage? {
    if data == "close" || data == "ping" || data == "pong" {
        return IpcMessageString(data: data)
    }
    
    let message = JSONParse(data, of: IpcMessageData.self)
    
    if message.type == .request {
        return JSONParse(data, of: IpcReqMessage.self)
    } else if message.type == .response {
        return JSONParse(data, of: IpcResMessage.self)
    } else if message.type == .stream_data {
        return JSONParse(data, of: IpcStreamData.self)
    } else if message.type == .stream_pull {
        return JSONParse(data, of: IpcStreamPull.self)
    } else if message.type == .stream_end {
        return JSONParse(data, of: IpcStreamEnd.self)
    } else if message.type == .stream_abort {
        return JSONParse(data, of: IpcStreamAbort.self)
    }
    
    return nil
}

