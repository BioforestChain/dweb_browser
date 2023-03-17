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

/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
class ReadableStreamIpc: Ipc {
    convenience init(remote: MicroModuleInfo, role: IPC_ROLE) {
        self.init(remote: remote, role: role.rawValue)
    }
    
    init(remote: MicroModuleInfo, role: String) {
        super.init()
        self.remote = remote
        self.role = role
        
        self.stream = ReadableStream(cid: role, onStart: { controller in
            self.controller = controller
        }, onPull: { (size, controller) in
            print("ON-PULL/\(controller.stream)", size)
        })
    }
    
    override func toString() -> String {
        super.toString() + "@ReadableStreamIpc"
    }
    
    private var controller: ReadableStream.ReadableStreamController?
    
    var stream: ReadableStream?
    private func enqueue(data: Data) async {
        controller?.enqueue(data)
    }
    private var _incomeStream: InputStream? = nil
    
    private var PONG_DATA: Data = {
        var pong = "pong".utf8Data()!
        return pong.count.toData() + pong
    }()
    
    /**
     * 输入流要额外绑定
     */
    
    func bindIncomeStream(request: Request) async {
        if _incomeStream != nil {
            fatalError("in come stream already binded.")
        }
        
        if support_message_pack {
            fatalError("还未实现 MessagePack 的编解码能力")
        }
        
        let po = PromiseOut3<()>(eventLoop: request.eventLoop)
        var data = Data()
//        let sequential = request.eventLoop.makeSucceededFuture(())
//        request.body.drain { body in
//            switch body {
//            case .buffer(let buffer):
//                data += Data(buffer.readableBytesView)
//                return sequential
//            case .error, .end:
//                po.resolve(())
//                return sequential
//            }
//        }
//        let buffer = try? request.body.collect().wait()
        
        print("waitPromise start")
        try? po.waitPromise()
        print("waitPromise end")
        _incomeStream = InputStream(data: data)
        
        while _incomeStream!.hasBytesAvailable {
            let size = _incomeStream!.readInt() ?? 0

            if size <= 0 { // 心跳包？
                continue
            }

            print("size/\(_incomeStream!)", size)
            var data = String(data: _incomeStream!.readData(size: size), encoding: .utf8)!

            let message = jsonToIpcMessage(data: data, ipc: self)
            if let message = message as? IpcMessageString {
                if message.data == "close" {
                    await close()
                } else if message.data == "ping" {
                    await enqueue(data: PONG_DATA)
                } else if message.data == "pong" {
                    print("PONG/\(_incomeStream!)")
                } else {
                    fatalError("unknown message: \(message.data)")
                }
            } else if message != nil {
                print("ON-MESSAGE/\(self)", message!)
                await self._messageSignal.emit((message!, self))
            } else {
                fatalError("message is nil")
            }
        }

        print("END/\(_incomeStream!)")
    }
    
    func bindIncomeStream(response: Response) async {
        
    }
    
    
    func bindIncomeStream(data: Data?) async {
//    func bindIncomeStream(body: CustomStringConvertible) {
        if _incomeStream != nil {
            fatalError("in come stream already binded.")
        }
        
        if support_message_pack {
            fatalError("还未实现 MessagePack 的编解码能力")
        }
        
        if data == nil {
            return
        }
        
        let po = PromiseOut<InputStream>()
        
        let task = Task {
            let stream = InputStream(data: data!)
            po.resolve(stream)
            while stream.hasBytesAvailable {
                let size = stream.readInt() ?? 0

                if size <= 0 { // 心跳包？
                    continue
                }

                print("size/\(stream)", size)
                var data = String(data: stream.readData(size: size), encoding: .utf8)!

                let message = jsonToIpcMessage(data: data, ipc: self)
                if let message = message as? IpcMessageString {
                    if message.data == "close" {
                        await close()
                    } else if message.data == "ping" {
                        await enqueue(data: PONG_DATA)
                    } else if message.data == "pong" {
                        print("PONG/\(stream)")
                    } else {
                        fatalError("unknown message: \(message.data)")
                    }
                } else if message != nil {
                    print("ON-MESSAGE/\(self)", message!)
                    await self._messageSignal.emit((message!, self))
                } else {
                    fatalError("message is nil")
                }
            }

            print("END/\(stream)")
        }

        _incomeStream = await po.waitPromise()
        await task.value
    }
    
    override func _doPostMessage(data: IpcMessage) async {
        var message = Data()
        if support_message_pack {
            do {
                let data = try MessagePackEncoder().encode(data)
                message += data
            } catch {
                print("ReadableStreamIpc _doPostMessage message_pack encode error: \(error.localizedDescription)")
            }
        } else {
            if let str = JSONStringify(data), let _data = str.fromUtf8() {
                message += _data
            }
        }
        
        print("post/\(stream)", message.count)
        await enqueue(data: message.count.toData() + message)
    }
    
    override func _doClose() async {
        controller?.close()
    }
}
