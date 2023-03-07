//
//  ReadableStreamIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import MessagePacker
import Combine
import Vapor

class ReadableStreamIpc: Ipc {
    init(remote: MicroModule, role: IPC_ROLE) {
        super.init()
        self.remote = remote
        self.role = role
    }
    
    private var writer: Data = Data()
    var stream: InputStream {
        get {
            InputStream(data: writer)
        }
    }
    
    private var _incomeStream: InputStream? = nil
    
    private lazy var PONG_DATA: ByteBuffer = {
        ByteBuffer.init(string: "pong")
    }()
    
    func bindIncomeStream(stream: InputStream) async {
        if _incomeStream != nil {
            fatalError("income stream already binded.")
        }
        
        if support_message_pack {
            fatalError("还未实现 MessagePack 的编解码能力")
        }
        
        _incomeStream = stream
        
        while stream.hasBytesAvailable {
            let bufferSize = 1024
            
            var data = Data()
            var buffer = [UInt8](repeating: 0, count: bufferSize)
            let bytesRead = stream.read(&buffer, maxLength: bufferSize)
            if bytesRead != bufferSize {
                fatalError("fail to read int(4 byte) in stream")
            }
            data.append(buffer, count: bytesRead)
            
            let message = String(data: data, encoding: .utf8)
            
            if message != nil {
                let result = jsonToIpcMessage(data: message!, ipc: self)
                if result != nil {
                    if result!.type == .unknown, let result = result as? IpcMessageString {
                        if result.data == "close" {
                            await self.close()
                        } else if result.data == "ping" {
                            
                        } else if result.data == "pong" {
                            print("PONG/\(stream)")
                        }
                    } else {
                        await self._messageSignal.emit((result!, self))
                    }
                }
            }
        }
    }
    
    override func _doPostMessage(data: IpcMessage) async {
        if support_message_pack {
            do {
                let data = try MessagePackEncoder().encode(data)
                writer.append(data)
            } catch {
                print("ReadableStreamIpc _doPostMessage message_pack encode error: \(error.localizedDescription)")
            }
        } else {
            let str = JSONStringify(data)
            
            if str != nil {
                let data = str!.to_utf8_data()
                writer.append(data!)
            }
        }
    }
    
    override func _doClose() async {}
}


