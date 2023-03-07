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

    /**
       * 是否支持使用 MessagePack 直接传输二进制
       * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
       * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
       * 否则 JSON 是通用的传输协议
       */
    
    
    var uid_acc = 0
    var supportMessagePack: Bool = false
    var uid: Int {
        let tmp = uid_acc
        uid_acc = uid_acc + 1
        return tmp
    }
    /**
       * 是否支持使用 Protobuf 直接传输二进制
       * 在网络环境里，protobuf 是更加高效的协议
       */
    var supportProtobuf: Bool = false
    /**
         * 是否支持结构化内存协议传输：
         * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
         */
    var supportRaw: Bool = false
    var support_binary: Bool  = false
//    {
//        return self.supportMessagePack || self.supportProtobuf
//    }
    var remote: MicroModule?
    var role: IPC_ROLE?
    
    private var _closed = false
    
    private var closed: Bool {
        return _closed
    }
    
    private var isClosed: Bool {
        return _closed
    }
    
    private var ipcMessage: OnIpcMessage?
    
    private var onIpcrequestMessage: OnIpcrequestMessage?
    
    var messageSignal: Signal<(IpcMessage,Ipc)>?
    var onMessage: ((@escaping OnIpcMessage) -> OffListener)!
    
    private let closeSignal = SimpleSignal()
    
    private(set) var reqresMap: [Int: PromiseOut<IpcResponse>] = [:]
    private var req_id_acc = 0
    private var inited_req_res: Bool = false
    
    private var messageResponse: IpcResponse?
    
    override init() {
        super.init()
        messageSignal = Signal<(IpcMessage,Ipc)>()
        onMessage = messageSignal!.listen
        
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
    
    func doPostMessage(data: IpcMessage) { }
    
    lazy var getOnRequestListener: ((@escaping OnIpcrequestMessage) -> OffListener) = {
        
        let signal = Signal<(IpcRequest, Ipc)>()
        
        _ = self.onMessage({ (request, ipc) in
            if let request = request as? IpcRequest {
                signal.emit((request, ipc))
            }
            return nil
        })

        return signal.listen
        
    }()
    
    func onRequest(cb: @escaping OnIpcrequestMessage) -> Any {
        
        return self.getOnRequestListener(cb)
    }
    
    func doClose() { }
    
    func closeAction() {
        guard !self._closed else { return }
        self._closed = true
        self.doClose()
        self.closeSignal.emit(())
        self.closeSignal.clear()
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
    
    func request(request: IpcRequest) -> IpcResponse? {
        self.postMessage(message: request)
        let result = PromiseOut<IpcResponse>()
        
        _ = self.onMessage({ (message, ipc) in
       
            if let req = message as? IpcResponse, req.req_id == request.req_id {
                result.resolver(req)
            }
            return nil
        })
        return result.waitPromise()
    }
    
    func request(request: URLRequest) -> Response? {
        self.request(request: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self))
        if self.messageResponse != nil {
            return self.messageResponse?.toResponse()
        }
        return nil
    }
    
    func onClose(cb: @escaping SimpleCallbcak) -> OffListener {
        return self.closeSignal.listen(cb)
    }
}

class initObj {
    
    var method: String?
    var body: Any?
    var headers: IpcHeaders?
}
