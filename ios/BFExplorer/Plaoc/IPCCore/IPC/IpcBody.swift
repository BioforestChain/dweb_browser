//
//  IpcBody.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import SwiftUI

class IpcBody: NSObject {

    private var ipc: Ipc?
    private var body: Body?
    private var rawData: RawData?
    
    override init() {
        
    }
    
    init(rawBody: RawData, ipc: Ipc?) {
        super.init()
        self.ipc = ipc
        self.rawData = rawBody
    }
    
    func bodyData() -> Any {
        return initBody()?.data
    }
    
    private func initBody() -> Body? {
        if self.body == nil {
            if self.rawData != nil, self.ipc != nil {
                let data = rawDataToBody.rawDataToBodyResult(rawBody: self.rawData!, ipc: self.ipc!)
                self.body = Body(data: data)
                if data is InputStream {
                    self.body?.stream = data as? InputStream
                } else if data is [UInt8] {
                    self.body?.u8a = data as? [UInt8]
                } else if data is String {
                    self.body?.text = data as? String
                }
            }
        }
        return self.body
    }
    
    func u8a() -> [UInt8]? {
        guard let body = self.initBody() else { return nil }
        var body_u8a = body.u8a
        if body_u8a == nil {
            if body.stream != nil {
                body_u8a = StreamFileManager.readData(stream: body.stream!)
            } else if body.text != nil {
                body_u8a = encoding.simpleEncoder(data: body.text!, encoding: .utf8)
            } else {
                print("invalid body type")
            }
            self.body?.u8a = body_u8a
        }
        return body_u8a
    }
    
    func stream() -> InputStream? {
        guard let body = self.initBody() else { return nil }
        var body_stream = body.stream
        if body_stream == nil {
            let binary = self.u8a() ?? []
            let data = Data(bytes: binary, count: binary.count)
            body_stream = InputStream(data: data)
            self.body?.stream = body_stream
        }
        return body_stream
    }
    
    func text() -> String? {
        guard let body = self.initBody() else { return nil }
        var body_text = body.text
        if body_text == nil {
            if self.u8a() != nil {
                body_text = encoding.simpleDecoder(data: self.u8a()!, encoding: .utf8)
                self.body?.text = body_text
            }
        }
        return body_text
    }
}


struct RawData {
    
    private(set) var result: Any?
    private(set) var type: IPC_RAW_BODY_TYPE?
    init(raw: IPC_RAW_BODY_TYPE, content: String) {
        self.type = raw
        analysisData(raw: raw, content: content)
    }
    
    private mutating func analysisData(raw: IPC_RAW_BODY_TYPE, content: String) {
        switch raw {
        case .TEXT:
            result = encoding.simpleEncoder(data:content, encoding:.utf8)
        case .BASE64:
            result = encoding.simpleEncoder(data:content, encoding:.base64)
        case .TEXT_STREAM_ID:
            result = ""
        case .BASE64_STREAM_ID:
            result = ""
        case .BINARY:
            var binary: [UInt8] = []
            for code in content.utf8 {
                binary.append(code)
            }
            result = binary
        case .BINARY_STREAM_ID:
            result = ""
        case .STREAM_ID:
            result = ""
        }
    }
}


struct Body {
    
    var data: Any?  
    var u8a: [UInt8]?
    var stream: InputStream?
    var text: String?
}
