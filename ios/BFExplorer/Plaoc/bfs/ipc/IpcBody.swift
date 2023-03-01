//
//  IpcBody.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation

class IpcBody {
    struct StreamData: Codable {
        var stream: Data
    }
    struct BodyHub: Codable {
        var text: String? = nil
        var stream: StreamData? = nil
        var u8a: Data? = nil
    }
    
    var bodyHub: BodyHub
    var metaBody: MetaBody
    var body: BodyHub
    
    init(metaBody: MetaBody?, body: BodyHub?) {
        self.metaBody = metaBody ?? MetaBody(type: .text, data: S_MetaBody())
        self.bodyHub = BodyHub()
        self.body = body ?? bodyHub
    }
    
    private lazy var _u8a: Data = {
        if bodyHub.u8a != nil {
            return bodyHub.u8a!
        } else if bodyHub.stream != nil {
            return bodyHub.stream!.stream
        } else if bodyHub.text != nil {
            return bodyHub.text!.to_b64_data()!
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func u8a() -> Data {
        return _u8a
    }
    
    private lazy var _stream: InputStream = {
        if bodyHub.stream != nil {
            return InputStream(data: bodyHub.stream!.stream)
        } else if bodyHub.u8a != nil {
            return InputStream(data: bodyHub.u8a!)
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func stream() -> InputStream {
        return _stream
    }
    
    private lazy var _text: String = {
        if bodyHub.text != nil {
            return bodyHub.text!
        } else if bodyHub.u8a != nil {
            return String(data: bodyHub.u8a!, encoding: .utf8)!
        } else {
            fatalError("invalid body type")
        }
    }()
    
    func text() -> String {
        return _text
    }
}
