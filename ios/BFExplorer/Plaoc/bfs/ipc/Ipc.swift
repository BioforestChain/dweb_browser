//
//  Ipc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation
import Vapor
import Combine

class Ipc {
    typealias IpcTupleCtor = () -> SIGNAL_CTOR
    typealias IpcTupleBool = () async -> Bool
    
    private static var uid_acc = 1
    var uid: Int = Ipc.uid_acc++
    /**
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     */
    var support_message_pack: Bool = false
    /**
     * 是否支持 Protobuf 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
     */
    var support_protobuf: Bool = false
    /** 是否支持 二进制 传输 */
    var suport_bianry: Bool {
        get {
            return support_message_pack || support_protobuf
        }
    }
    /**
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     */
    var supportRaw: Bool = false
    var remote: MicroModule
    var role: IPC_ROLE
    
    init() {
        remote = NativeMicroModule()
        role = IPC_ROLE.client
//        onMessage = _messageSignal.listen
    }
    
    func toString() -> String {
        "#i\(uid)"
    }

    internal var _messageSignal = Signal<(IpcMessage, Ipc)>()
    func postMessage(message: IpcMessage) async -> Void {
        if self._closed {
            return
        }
        
        await self._doPostMessage(data: message)
    }
    
    func postResponse(req_id: Int, response: Response) async {
        await postMessage(message: IpcResponse.fromResponse(req_id: req_id, response: response, ipc: self).ipcResMessage)
    }
    
//    var onMessage: ((@escaping OnIpcMessage) -> IpcTupleBool)
    func onMessage(cb: @escaping OnIpcMessage) -> IpcTupleBool {
        return _messageSignal.listen(cb)
    }
    func _doPostMessage(data: IpcMessage) async {}
    
    private lazy var _getOnRequestListener = {
        let signal = Signal<(IpcRequest, Ipc)>()
        _ = _messageSignal.listen { (message, ipc) in
            if let message = message as? IpcReqMessage {
                await signal.emit((message.toIpcRequest(), ipc))
            }
            
            return nil
        }

        return signal.listen
    }()
    
    func onRequest(cb: @escaping OnIpcRequestMessage) -> IpcTupleBool {
        return _getOnRequestListener(cb)
    }
    
    private var _closed = false
    private var _closeSignal = Signal<()>()
    
    func _doClose() async {}
    
    func close() async {
        if self._closed {
            return
        }
        
        self._closed = true
        await self._doClose()
        await self._closeSignal.emit(())
    }
    
    func onClose(cb: @escaping IpcTupleCtor) -> IpcTupleBool {
        return self._closeSignal.listen(cb)
    }
    
    func request(ipcRequest: IpcRequest) async -> IpcResponse {
        await self.postMessage(message: ipcRequest.ipcReqMessage)
//        let po = PromiseOut<IpcResponse>()
        let task = Task(priority: .userInitiated) {
            let po = PromiseOut<IpcResponse>()
            _ = self.onMessage { (message, ipc) in
                if let message = message as? IpcResMessage, ipcRequest.req_id == message.req_id {
                    po.resolve(message.toIpcResponse())
//                    return message.toIpcResponse()
                }
                
                return .OFF
            }
            
            return await po.waitPromise()
        }
        
        return await task.value
//        return await po.waitPromise()
//        return IpcResponse(req_id: 1, statusCode: 200, headers: IpcHeaders(["Content-Type": "text/plain"]), body: .init(metaBody: nil, body: .init(text:"")))
    }
    
    func request(request: Request) async -> Response {
//        let task = Task {
//            return await self.request(ipcRequest: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self)).toResponse()
//        }
//
//        return await task.value
        return await self.request(ipcRequest: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self)).toResponse()
    }
    private static var _req_id_acc = 0
    func allocReqId() -> Int {
        return Ipc._req_id_acc++
    }
    
}

// 用于Set中判断是否相同
extension Ipc: Hashable {
    func hash(into hasher: inout Hasher) {
        hasher.combine(uid)
    }
    
    static func ==(lhs: Ipc, rhs: Ipc) -> Bool {
        return lhs.uid == rhs.uid
    }
}
