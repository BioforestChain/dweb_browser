//
//  ReadableStreamIpc.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit
import HandyJSON

class ReadableStreamIpc: Ipc {

    var controller: ReadableStreamController?
    var incomeStream: InputStream?
    
    var stream: ReadableStream! {
        return ReadableStream(cid: self.role) { controller in
            self.controller = controller
        } onPull: { size, controller in
            
        }
    }
    
    private var PONG_DATA: Data? {
        guard var data = "pong".utf8Data() else { return nil }
        return data.count.toByteArray() + data
    }
    
    init(remote: MicroModuleInfo, role: IPC_ROLE) {
        super.init()
        self.remote = remote
        self.role = role.rawValue
    }
    
    override func toString() -> String {
        return super.toString() + "@ReadableStreamIpc"
    }
    
    func enqueue(data: Data) {
        controller?.enqueue(data)
    }
    
    func bindIncomeStream(stream: InputStream?, coroutineName: String) {
        guard stream != nil else { return }
        guard self.incomeStream == nil else { return }
        guard !supportMessagePack else { return }
        
        self.incomeStream = stream
        
        Task {
            
            while stream!.hasBytesAvailable {
                let size = stream?.readInt() ?? 0
                if size <= 0 {
                    continue
                }
                
                let bytes = stream!.readByteArray(size: size)
                let data = Data(bytes: bytes, count: bytes.count)
                var chunk = String(data: data, encoding: .utf8) ?? ""
                
                let message = jsonToIpcMessage.jsonToIpcMessage(data: chunk, ipc: self)
                if let content = message as? String {
                    if content == "close" {
                        closeAction()
                    } else if content == "ping" {
                        if PONG_DATA != nil {
                            enqueue(data: PONG_DATA!)
                        }
                    } else if content == "pong" {
                        print("PONG/\(stream)")
                    }
                } else if let message = message as? IpcMessage {
                    messageSignal?.emit((message,self))
                }else {
                    fatalError("message is nil")
                }
            }
        }
    }
    
    override func doPostMessage(data: IpcMessage) {
        
        var message: [UInt8] = []
//        if supportMessagePack {
//            if let jsonString = data.toJSONString(), let messageData = jsonString.utf8Data() {
//                message = messageData
//            }
//        } else {
//            if let jsonString = data.toJSONString(), let messageData = jsonString.utf8Data() {
//                message = messageData
//            }
//        }
        if let jsonString = data.toJSONString(), let messageData = jsonString.fromUtf8() {
            message = messageData
        }
        enqueue(data: Data(message))
    }
    
    override func doClose() {
        controller?.close()
    }
}
