//
//  Ipc.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import Combine
import Vapor

class Ipc: NSObject {

    private var uid_acc = 1
    private var req_id_acc = 0
    /**
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     */
    var supportMessagePack: Bool = false
    var uid: Int {
        let tmp = uid_acc
        uid_acc = uid_acc + 1
        return tmp
    }
    /**
        * 是否支持 Protobuf 协议传输：
        * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
        */
    var supportProtobuf: Bool = false
    /**
         * 是否支持结构化内存协议传输：
         * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
         */
    var supportRaw: Bool = false
    /** 是否支持 二进制 传输 */
    var supportBinary: Bool  = false
    var remote: MicroModuleInfo?
    var role: String?
    
    private var _closed = false
    
    private var closed: Bool {
        return _closed
    }
    
    private var isClosed: Bool {
        return _closed
    }
    
    private var _destroyed = false
    
    var isDestroy: Bool {
        return _destroyed
    }
    
    private var ipcMessage: OnIpcMessage?
    
    private var onIpcrequestMessage: OnIpcrequestMessage?
    
    var messageSignal: Signal<(IpcMessage,Ipc)>?
//    var onMessage: ((@escaping OnIpcMessage) -> OffListener)!
    
    private let closeSignal = SimpleSignal()
    private let destroySignal = SimpleSignal()
    
    private(set) var reqresMap: [Int: PromiseOut<IpcResponse>] = [:]
    
    private var inited_req_res: Bool = false
    
    private var messageResponse: IpcResponse?
    
    override init() {
        super.init()
        messageSignal = Signal<(IpcMessage,Ipc)>()
        
    }
    
    func asRemoteInstance() -> MicroModule? {
        if let module = remote as? MicroModule {
            return module
        }
        return nil
    }
    
    func toString() -> String {
        return "#i\(uid)"
    }
    
    func postMessage(message: IpcMessage) {
        guard !_closed else { return }
        doPostMessage(data: message)
        
    }
    
    func postResponse(req_id: Int, response: Response) {
        guard let ipcRes = IpcResponse.fromResponse(req_id: req_id, response: response, ipc: self) else { return }
        postMessage(message: ipcRes)
    }
    
    func onMessage(cb: @escaping OnIpcMessage) -> OffListener {
        return messageSignal!.listen(cb)
    }
    
    func doPostMessage(data: IpcMessage) { }
    
    lazy var requestSignal: Signal<(IpcRequest, Ipc)> = {
        let signal = Signal<(IpcRequest, Ipc)>()
        
        _ = self.messageSignal?.listen({ message,ipc in
            if let request = message as? IpcRequest {
                signal.emit((request, ipc))
            }
            return nil
        })
        return signal
    }()
    
    func onRequest(cb: @escaping OnIpcrequestMessage) -> OffListener {
        
        return requestSignal.listen(cb)
    }
    
    lazy var eventSignal: Signal<(IpcEvent, Ipc)> = {
        let signal = Signal<(IpcEvent, Ipc)>()
        
        _ = self.messageSignal?.listen({ message,ipc in
            if let event = message as? IpcEvent {
                signal.emit((event, ipc))
            }
            return nil
        })
        return signal
    }()
    
    func onEvent(cb: @escaping OnIpcEventMessage) -> OffListener {
        
        return eventSignal.listen(cb)
    }
    
    func onClose(cb: @escaping SimpleCallbcak) -> OffListener {
        
        return closeSignal.listen(cb)
    }
    
    func onDestroy(cb: @escaping SimpleCallbcak) -> OffListener {
        
        return destroySignal.listen(cb)
    }
    
    func doClose() { }
    
    func closeAction() {
        guard !self._closed else { return }
        self._closed = true
        self.doClose()
        self.closeSignal.emit(())
        self.closeSignal.clear()
        // 关闭的时候会自动触发销毁
//        destory
    }
    
    func destroy(close: Bool = true) {
        guard !_destroyed else { return }
        _destroyed = true
        if close {
            self.closeAction()
        }
        
        self.destroySignal.emit(())
        self.destroySignal.clear()
    }
    
    func allocReqId() -> Int {
        let tmp = uid_acc
        req_id_acc = req_id_acc + 1
        return tmp
    }
    
    
    func request(urlString: String) -> Response? {
        
        guard let url = URL(string: urlString) else { return nil }
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        return self.request(request: req)
    }
    
    func request(url: URL) -> Response? {

        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        return self.request(request: req)
    }
    
    func request(ipcRequest: IpcRequest) -> IpcResponse? {
        self.postMessage(message: ipcRequest)
        let result = PromiseOut<IpcResponse>()
        
        _ = self.onMessage(cb: { (message, ipc) in
       
            if let req = message as? IpcResponse, req.req_id == ipcRequest.req_id {
                result.resolver(req)
            }
        })
        return result.waitPromise()
    }
    
    func request(request: URLRequest) -> Response? {
        let response = self.request(ipcRequest: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self))
        return response?.toResponse()
    }
}

