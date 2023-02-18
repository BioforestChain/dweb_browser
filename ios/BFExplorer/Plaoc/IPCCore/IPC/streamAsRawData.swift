//
//  streamAsRawData.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/14.
//

import UIKit

class streamAsRawData {
    
    static func streamAsRawData(streamId: String, stream: InputStream, ipc: Ipc) {
        
//        stream.open()
//        
//        defer {
//            stream.close()
//        }
//        
//        let sender = postStreamData(streamId: streamId, reader: stream, ipc: ipc) {
////            off()
//            self.off(streamId: streamId, stream: stream, ipc: ipc)
//        }
        
//        var emptyResult: (() -> Bool)!
//
//        let off = ipc.onMessage({ (request, ipc) in
//            Task.detached {
//                if let request = request as? IpcStreamPull, request.stream_id == streamId {
////                    await sender.next()
//
//
//                } else if let request = request as? IpcStreamAbort, request.stream_id == streamId {
//                    print("abort")
//                }
//            }
//
//            return emptyResult
//        })
        
    }
    
//    func off(streamId: String, stream: InputStream, ipc: Ipc) {
//
//        var emptyResult: (() -> Bool)!
//        let fuc = ipc.onMessage({ (request, ipc) in
//            Task.detached {
//                if let request = request as? IpcStreamPull, request.stream_id == streamId {
//
//
//                } else if let request = request as? IpcStreamAbort, request.stream_id == streamId {
//                    print("abort")
//                }
//            }
//
//            return emptyResult
//        })
//    }
    
    static func postStreamData(streamId: String, reader: InputStream, ipc: Ipc, onDone: @escaping () -> Void) -> AsyncStream<Any> {
        
        var buffer = [UInt8](repeating: 0, count: 1024)
        
        return AsyncStream<Any> { continuation in
            while true {
                let bytesRead = reader.read(&buffer, maxLength: buffer.count)
                if bytesRead <= 0 {
                    continuation.finish()
                    break
                }
                ipc.postMessage(message: IpcStreamData.fromBinary(ipc: ipc, stream_id: streamId, data: Array(buffer[0..<bytesRead])))
                continuation.yield(())
            }
            ipc.postMessage(message: IpcStreamEnd(stream_id: streamId))
            onDone()
        }
    }
}

 
