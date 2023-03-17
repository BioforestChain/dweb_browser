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
    typealias IpcTupleCtor = VoidCallback<SIGNAL_CTOR>
    typealias IpcTupleBool = AsyncVoidCallback<Bool>
    typealias IpcMessageArgs = (IpcMessage, Ipc)
    typealias OnIpcMessage = AsyncCallback<IpcMessageArgs, Any>
    typealias IpcRequestMessageArgs = (IpcReqMessage, Ipc)
    typealias OnIpcRequestMessage = AsyncCallback<IpcRequestMessageArgs, Any>
    typealias IpcEventMessageArgs = (IpcEvent, Ipc)
    typealias OnIpcEventMessage = AsyncCallback<IpcEventMessageArgs, Any>
    
    private static var uid_acc = 1
    private static var req_id_acc = 0
    
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
    
    /**
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     */
    var support_raw: Bool = false
    
    /** 是否支持 二进制 传输 */
    var support_bianry: Bool = false
    
    var remote: MicroModuleInfo = MicroModule()
    
    func asRemoteInstance() -> MicroModule? {
        if let remote = remote as? MicroModule {
            return remote
        } else {
            return nil
        }
    }
    
    var role: String = ""
    
    func toString() -> String {
        "#i\(uid)"
    }
    
    func postMessage(message: IpcMessage) async {
        if self._closed {
            return
        }
        
        await self._doPostMessage(data: message)
    }
    
    func postResponse(req_id: Int, response: Response) async {
        await postMessage(message: IpcResponse.fromResponse(req_id: req_id, response: response, ipc: self).ipcResMessage)
    }
    
    internal var _messageSignal = Signal<IpcMessageArgs>()
    
    func onMessage(cb: @escaping OnIpcMessage) -> IpcTupleBool {
        return _messageSignal.listen(cb)
    }
    func _doPostMessage(data: IpcMessage) async {}
    
    private lazy var _requestSignal = {
        let signal = Signal<IpcRequestMessageArgs>()
        _ = _messageSignal.listen { (message, ipc) in
            if let message = message as? IpcReqMessage {
                await signal.emit((message, ipc))
            }
            
            return nil
        }

        return signal.listen
    }()
    
    func onRequest(cb: @escaping OnIpcRequestMessage) -> IpcTupleBool {
        return _requestSignal(cb)
    }
    
    private lazy var _eventSignal = {
        let signal = Signal<IpcEventMessageArgs>()
        _ = _messageSignal.listen { (message, ipc) in
            if let message = message as? IpcEvent {
                await signal.emit((message, ipc))
            }
            
            return nil
        }

        return signal.listen
    }()
    
    func onEvent(cb: @escaping OnIpcEventMessage) -> IpcTupleBool {
        return _eventSignal(cb)
    }
    
    
    func _doClose() async {}
    private var _closed = false

    func close() async {
        if self._closed {
            return
        }
        
        self._closed = true
        await self._doClose()
        await self._closeSignal.emit(())
        await self._closeSignal.clear()
    }
    
    private var _closeSignal = Signal<()>()
    
    var isClosed: Bool {
        get {
            _closed
        }
    }
    
    func onClose(cb: @escaping IpcTupleCtor) -> IpcTupleBool {
        return self._closeSignal.listen(cb)
    }
    
    deinit {
        if !_closed {
            Task {
                await close()
            }
        }
    }
    
    func request(ipcRequest: IpcRequest) async -> IpcResponse {
        await self.postMessage(message: ipcRequest.ipcReqMessage)
//        let po = PromiseOut<IpcResponse>()
        let task = Task {
            let po = PromiseOut<IpcResponse>()
            _ = self.onMessage { (message, ipc) in
                if let message = message as? IpcResMessage, ipcRequest.req_id == message.req_id {
                    po.resolve(message.toIpcResponse(ipc: ipc))
//                    return message.toIpcResponse()
                }

                return SIGNAL_CTOR.OFF
            }

            return await po.waitPromise()
        }

        return await task.value
//        return await po.waitPromise()
    }
    
    func request(request: Request) async -> Response {
        return await self.request(ipcRequest: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self)).toResponse()
    }
    
    func allocReqId() -> Int {
        return Ipc.req_id_acc++
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
