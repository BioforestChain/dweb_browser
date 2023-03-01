//
//  IpcBodySender.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation

class IpcBodySender: IpcBody {
    private let ipc: Ipc
    
    init(body: BodyHub, ipc: Ipc) {
        self.ipc = ipc
        super.init(metaBody: nil, body: body)
        
        if body.text != nil {
            bodyHub = BodyHub(text: body.text)
        } else if body.u8a != nil {
            bodyHub = BodyHub(u8a: body.u8a)
        } else if body.stream != nil {
            bodyHub = BodyHub(stream: body.stream)
        }
        
        metaBody = IpcBodySender.bodyAsRawData(body: body, ipc: ipc)
    }
    
    private static var streamIdWM: [(InputStream,String)] = []
    
    private static var stream_id_acc = 1
    static func getStreamId(stream: InputStream) -> String {
        if let i = streamIdWM.firstIndex(where: { $0.0 == stream }) {
            return streamIdWM[i].1
        } else {
            let stream_id = "rs_\(stream_id_acc++)"
            streamIdWM.append((stream, stream_id))
            return stream_id
        }
    }
    
    private static func bodyAsRawData(body: BodyHub, ipc: Ipc) -> MetaBody {
        if body.text != nil {
            return textAsRawData(text: body.text!, ipc: ipc)
        } else if body.u8a != nil {
            return binaryAsRawData(binary: body.u8a!, ipc: ipc)
        } else if body.stream != nil {
            return streamAsRawData(stream: body.stream!, ipc: ipc)
        } else {
            fatalError("invalid body type \(body)")
        }
    }
    
    private static func textAsRawData(text: String, ipc: Ipc) -> MetaBody {
        return MetaBody(type: .text, data: S_MetaBody(string: text))
    }
    
    private static func binaryAsRawData(binary: Data, ipc: Ipc) -> MetaBody {
        if ipc.suport_bianry {
            return MetaBody(type: .binary, data: S_MetaBody(data: binary))
        } else {
            return MetaBody(type: .base64, data: S_MetaBody(string: binary.base64EncodedString()))
        }
    }
    
    private static func streamAsRawData(stream: StreamData, ipc: Ipc) -> MetaBody {
        let stream = InputStream(data: stream.stream)
        let stream_id = getStreamId(stream: stream)
        
        _ = ipc.onMessage { message, _ in
            if let message = message as? IpcStreamPull, message.stream_id == stream_id {
                DispatchQueue.global(qos: .background).async {
                    var desiredSize = message.desiredSize
                    stream.open()
                    defer {
                        stream.close()
                    }
                    while desiredSize != nil && desiredSize! > 0 {
                        if stream.hasBytesAvailable {
                            var data = Data()
                            let bufferSize = 1024
                            let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
                            stream.read(buffer, maxLength: bufferSize)
                            data.append(buffer, count: bufferSize)
                            ipc.postMessage(message: IpcStreamData.fromBinary(ipc: ipc, stream_id: stream_id, data: data))
                            desiredSize! -= bufferSize
                            buffer.deallocate()
                        }
                        
                        ipc.postMessage(message: IpcStreamPull(stream_id: stream_id, desiredSize: nil))
                    }
                }
            } else if let message = message as? IpcStreamAbort, message.stream_id == stream_id {
                print("ipcStreamAbort stream_id: \(stream_id)")
            }
            
            return nil
        }
        
        if ipc.suport_bianry {
            return MetaBody(type: .binary_stream_id, data: S_MetaBody(string: stream_id))
        } else {
            return MetaBody(type: .base64_stream_id, data: S_MetaBody(string: stream_id))
        }
    }
}
