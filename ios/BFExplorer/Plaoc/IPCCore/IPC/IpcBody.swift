//
//  IpcBody.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import SwiftUI

class IpcBody {

    var ipc: Ipc?
    var bodyHub: Body?
    var metaBody: MetaBody?
    
    var body: Any {
        return bodyHub?.data
    }
    
    func u8a() -> [UInt8]? {
        guard bodyHub != nil else { return nil }
        var body_u8a = bodyHub?.u8a
        if body_u8a == nil {
            if bodyHub?.stream != nil {
                body_u8a = StreamFileManager.readData(stream: bodyHub!.stream!)
            } else if bodyHub?.text != nil {
                body_u8a = encoding.simpleEncoder(data: bodyHub!.text!, encoding: .base64)
            } else {
                print("invalid body type")
            }
            self.bodyHub?.u8a = body_u8a
        }
        return body_u8a
    }
    
    func stream() -> InputStream? {
        guard bodyHub != nil else { return nil }
        var body_stream = bodyHub?.stream
        if body_stream == nil {
            let binary = self.u8a() ?? []
            let data = Data(bytes: binary, count: binary.count)
            body_stream = InputStream(data: data)
            self.bodyHub?.stream = body_stream
        }
        return body_stream
    }
    
    func text() -> String? {
        guard bodyHub != nil else { return nil }
        var body_text = bodyHub?.text
        if body_text == nil {
            if self.u8a() != nil {
                body_text = encoding.simpleDecoder(data: self.u8a()!, encoding: .utf8)
                self.bodyHub?.text = body_text
            }
        }
        return body_text
    }
}


struct Body {
    
    var data: Any?  
    var u8a: [UInt8]?
    var stream: InputStream?
    var text: String?
}
