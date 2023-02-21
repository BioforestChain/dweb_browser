//
//  streamAsRawData.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/14.
//

import UIKit

class streamAsRawData {
    
    static func streamAsRawData(streamId: String, stream: InputStream, ipc: Ipc) {
        
        stream.open()
        
        defer {
            stream.close()
        }
        
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }

        _ = ipc.onMessage({ (request, ipc) in
            
            if let request = request as? IpcStreamPull, request.stream_id == streamId {
                DispatchQueue.global().async {
                    let length = stream.read(buffer, maxLength: bufferSize)
                    let data = Data(bytes: buffer, count: length)
                    if length <= 0 {
                        ipc.postMessage(message: IpcStreamEnd(stream_id: streamId))
                    } else {
                        ipc.postMessage(message: IpcStreamData.fromBinary(ipc: ipc, stream_id: streamId, data: [UInt8](data)))
                    }
                }
            } else if let request = request as? IpcStreamAbort, request.stream_id == streamId {
                stream.close()
            }
            return ipc.messageSignal?.closure
        })
        
    }
}
