//
//  ReadableStreamIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit

class ReadableStreamIpc: Ipc {

    private var controller: ReadableStreamController?
    private var incomeStream: InputStream?
    private var stream: ReadableStream!
    
    init(remote: MicroModule, role: IPC_ROLE) {
        super.init()
        
        self.stream = ReadableStream().startLoad { controller in
            self.controller = controller
        } onPull: { desiredSize, controller in
            
        }
        
    }
    
    @inline(__always) func enqueue(data: [UInt8]) {
        controller?.enqueue(byteArray: data)
    }
    
    func bindIncomeStream(stream: InputStream, coroutineName: String) {
        guard self.incomeStream == nil else { return }
        guard !supportMessagePack else { return }
        
        let j = Task {
            try? Task.checkCancellation()
            try? await Task.sleep(nanoseconds: 10000)
            print("LIVE/$stream")
        }
        
        self.incomeStream = stream
        
        Task {
            
            let bufferSize = 1024
            let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
            defer {
                buffer.deallocate()
            }
            // 如果通道关闭并且没有剩余字节可供读取，则返回 true
            while stream.hasBytesAvailable {
                let length = stream.read(buffer, maxLength: bufferSize)
                if length <= 0 {
                    continue
                }
                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                let data = Data(bytes: buffer, count: length)
                let chunk = String(data: data, encoding: .utf8) ?? ""
                
                
            }
        }
        
    }
}
