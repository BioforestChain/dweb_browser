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
func JSONParse<T: Codable>(_ str: String) -> T? {
    do {
        let jsonData = str.utf8Data()
        
        if jsonData == nil {
            return nil
        }
        
        return try JSONDecoder().decode(T.self, from: jsonData!)
    } catch {
        fatalError("data JSONParse error: \(str)")
    }
}

func jsonToIpcMessage(data: String, ipc: Ipc) -> IpcMessage? {
    if data == "close" || data == "ping" || data == "pong" {
        return IpcMessageString(data: data)
    }
    
    let jsonData = data.utf8Data()
    
    if jsonData == nil {
        return nil
    }
    
    do {
        let decoder = JSONDecoder()
        let message = try decoder.decode(IpcMessageData.self, from: jsonData!)
        
        if message.type == .request {
            let req = try decoder.decode(IpcReqMessage.self, from: jsonData!)
            return req
        } else if message.type == .response {
            let res = try decoder.decode(IpcResMessage.self, from: jsonData!)
            return res
        } else if message.type == .stream_data {
            return try decoder.decode(IpcStreamData.self, from: jsonData!)
        } else if message.type == .stream_pull {
            return try decoder.decode(IpcStreamPull.self, from: jsonData!)
        } else if message.type == .stream_end {
            return try decoder.decode(IpcStreamEnd.self, from: jsonData!)
        } else if message.type == .stream_abort {
            return try decoder.decode(IpcStreamAbort.self, from: jsonData!)
        }
        
        return nil
    } catch {
        return nil
    }
}

