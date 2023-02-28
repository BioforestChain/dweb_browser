//
//  IpcBodySender.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit

class IpcBodySender: IpcBody {

    override var body: Any {
        return bodySender
    }
    
    
    private var streamIdWM: [InputStream: String] = [:]
    private var stream_id_acc = 1
    private var bodySender: Any?
    
    init(body: Any, ipc: Ipc) {
        super.init()
        self.bodySender = body
        self.ipc = ipc
        self.bodyHub = hub
        self.metaBody = bodyAsRawData(body: body, ipc: ipc)
    }
    
    lazy private var hub: Body = {
        var body = Body()
        body.data = self.body
        if let str = self.body as? String {
            body.text = str
        } else if let bytes = self.body as? [UInt8] {
            body.u8a = bytes
        } else if let stream = self.body as? InputStream {
            body.stream = stream
        }
        return body
    }()
    
    func getStreamId(stream: InputStream) -> String {
        let tmp = stream_id_acc
        stream_id_acc += 1
        return streamIdWM[stream] ?? "rs-\(tmp)"
    }
    
    private func bodyAsRawData(body: Any, ipc: Ipc) -> MetaBody? {
        
        if let content = body as? String {
            return textAsRawData(text: content, ipc: ipc)
        } else if let bytes = body as? [UInt8] {
            return binaryAsRawData(binary: bytes, ipc: ipc)
        } else if let stream = body as? InputStream {
            return streamAsRawData(stream: stream, ipc: ipc)
        }
        return nil
    }
    
    private func textAsRawData(text: String, ipc: Ipc) -> MetaBody {
        return MetaBody(type: .TEXT, data: text)
    }
    
    private func binaryAsRawData(binary: [UInt8], ipc: Ipc) -> MetaBody {
        if ipc.support_binary {
            return MetaBody(type: .BINARY, data: binary)
        }
        return MetaBody(type: .BASE64, data: encoding.simpleDecoder(data: binary, encoding: .base64))
    }
    
    private func streamAsRawData(stream: InputStream, ipc: Ipc) -> MetaBody {
        let stream_id = getStreamId(stream: stream)
        
        _ = ipc.onMessage({ (message, _) in
       
            if let request = message as? IpcStreamPull, request.stream_id == stream_id {
                Task {
                    var desiredSize = request.desiredSize ?? 0
                    
                    while(desiredSize > 0) {
                        let bufferSize = 1024
                        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
                        defer {
                            buffer.deallocate()
                        }
                        
                        let length = stream.read(buffer, maxLength: bufferSize)
                        let data = Data(bytes: buffer, count: length)
                        
                        if length > 0 {
                            let binary = [UInt8](data)
                            ipc.postMessage(message: IpcStreamData.fromBinary(ipc: ipc, stream_id: stream_id, data: binary))
                            desiredSize -= length
                        } else {
                            ipc.postMessage(message: IpcStreamEnd(stream_id: stream_id))
                            break
                        }
                    }
                }
            } else if let request = message as? IpcStreamAbort, request.stream_id == stream_id {
                stream.close()
            }
            return ipc.messageSignal?.closure
        })
        if ipc.support_binary {
            return MetaBody(type: .BINARY_STREAM_ID, data: stream_id)
        } else {
            return MetaBody(type: .BASE64_STREAM_ID, data: stream_id)
        }
    }
}
