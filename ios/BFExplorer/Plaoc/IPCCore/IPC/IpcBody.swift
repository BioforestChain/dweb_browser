//
//  IpcBody.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import SwiftUI

var raw_ipcBody_WMap = NSMutableDictionary()
var metaId_receiverIpc_Map: [String:Ipc] = [:]
var metaId_ipcBodySender_Map: [String: IpcBodySender] = [:]

class IpcBody {

    var ipc: Ipc?
//    var bodyHub: BodyHub?
    var metaBody: MetaBody?
    
    var raw: Any
    
    init() {
        self.raw = ""//bodyHub?.data
    }
    
    func u8a() -> [UInt8]? {
        return nil
//        var body_u8a = bodyHub?.u8a
//        if body_u8a == nil {
//            if bodyHub?.stream != nil {
//                body_u8a = bodyHub?.stream!.readByteArray()
//            } else if bodyHub?.text != nil {
//                body_u8a = bodyHub?.text!.fromBase64()
//            } else {
//                print("invalid body type")
//                return nil
//            }
//            self.bodyHub?.u8a = body_u8a
//        }
//        raw_ipcBody_WMap[body_u8a] = self
//        return body_u8a
    }
    
    func stream() -> InputStream? {
        return nil
//        var body_stream = bodyHub?.stream
//        if body_stream == nil {
//            let binary = self.u8a() ?? []
//            let data = Data(bytes: binary, count: binary.count)
//            body_stream = InputStream(data: data)
//            self.bodyHub?.stream = body_stream
//        }
//        raw_ipcBody_WMap[body_stream] = self
//        return body_stream
    }
    
    func text() -> String? {
        return nil
//        var body_text = bodyHub?.text
//        if body_text == nil {
//            if self.u8a() != nil {
//                let binary = self.u8a() ?? []
//                body_text = binary.toUtf8()
//                self.bodyHub?.text = body_text
//            }
//        }
//        raw_ipcBody_WMap[body_text] = self
//        return body_text
    }
}


struct BodyHub {
    
    var data: Any?  
    var u8a: [UInt8]?
    var stream: InputStream?
    var text: String?
    
    init(data: Any? = nil, u8a: [UInt8]? = nil, stream: InputStream? = nil, text: String? = nil) {
        self.data = data
        self.u8a = u8a
        self.stream = stream
        self.text = text
    }
}

