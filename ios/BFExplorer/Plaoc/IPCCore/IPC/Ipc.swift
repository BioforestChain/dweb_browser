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
    
    
    var ipc_uid_acc = 0
    var supportMessagePack: Bool = false
    var uid: Int {
        let tmp = ipc_uid_acc
        ipc_uid_acc = ipc_uid_acc + 1
        return tmp
    }
    /**
       * 是否支持使用 Protobuf 直接传输二进制
       * 在网络环境里，protobuf 是更加高效的协议
       */
    var supportProtobuf: Bool = false
    
    var support_binary: Bool {
        return self.supportMessagePack || self.supportProtobuf
    }
    var remote: MicroModule?
    var role: IPC_ROLE?
    
    private var closed: Bool = false
    
    private var ipcMessage: OnIpcMessage?
    
    private var onIpcrequestMessage: OnIpcrequestMessage?
    
    var messageSignal: Signal<(IpcMessage,Ipc)>?
    var onMessage: ((@escaping OnIpcMessage) -> GenericsClosure<OnIpcMessage>)!
    
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
        guard !closed else { return }
        doPostMessage(data: message)
        
    }
    
    func doPostMessage(data: IpcMessage) { }
    
    lazy var getOnRequestListener: ((@escaping OnIpcrequestMessage) -> GenericsClosure<OnIpcrequestMessage>) = {
        
        let signal = Signal<(IpcRequest, Ipc)>()
        
        _ = self.onMessage({ (request, ipc) in
            if let request = request as? IpcRequest {
                signal.emit((request, ipc))
            }
            return self.messageSignal?.closure
        })

        return signal.listen
        
    }()
    
    func onRequest(cb: @escaping OnIpcrequestMessage) -> Any {
        
        return self.getOnRequestListener(cb)
    }
    
    func doClose() { }
    
    func closeAction() {
        guard !self.closed else { return }
        self.closed = true
        self.doClose()
        self.closeSignal.emit(())
    }
    
    func allocReqId() -> Int {
        let tmp = ipc_uid_acc
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
    
    func request(request: IpcRequest) {
        self.postMessage(message: request)
        let result = PassthroughSubject<IpcResponse, MyError>()
        
        _ = self.onMessage({ (message, ipc) in
       
            if let req = message as? IpcResponse, req.req_id == request.req_id {
                Task {
                    result.send(req)
                }
            }
            return ipc.messageSignal?.closure
        })
        
        _ = result.sink { complete in
            
        } receiveValue: { value in
            self.messageResponse = value
        }
    }
    
    func request(request: URLRequest) -> Response? {
        self.request(request: IpcRequest.fromRequest(req_id: allocReqId(), request: request, ipc: self))
        if self.messageResponse != nil {
            return self.messageResponse?.asResponse()
        }
        return nil
    }
    
    func responseBy(ipc: Ipc, byIpcRequest: IpcRequest) {
        
        guard byIpcRequest.asRequest() != nil else { return }
        guard let response = ipc.request(request: byIpcRequest.asRequest()!) else { return }
        guard let resultResponse = IpcResponse.fromResponse(req_id: byIpcRequest.req_id, response: response, ipc: ipc)  else { return }
        
        postMessage(message: resultResponse)
    }
    /*
    private func initReqRes() {
        guard !inited_req_res else { return }
        self.inited_req_res = true
        
        _ = self.onMessage({ (request, ipc) in
       
            if let request = request as? IpcResponse {
                let response_po = self.reqresMap[request.req_id]
                if response_po != nil {
                    self.reqresMap.removeValue(forKey: request.req_id)
                    response_po?.resolver(request)
                } else {
                    print("no found response by req_id: \(request.req_id)")
                }
            }
            return self.messageSignal?.closure
        })
    }
    
    func request(url: String, obj: initObj) -> Promise<IpcResponse>? {
        let req_id = self.allocReqId()
        let method = obj.method ?? "GET"
        let headers = obj.headers ?? IpcHeaders()
        let ipcRequest: IpcRequest
        
        if obj.body is [UInt8] {
            ipcRequest = IpcRequest.fromBinary(binary: obj.body as! [UInt8], req_id: req_id, method: method, urlString: url, headers: headers, ipc: self)
        } else if obj.body is InputStream {
            ipcRequest = IpcRequest.fromStream(stream: obj.body as! InputStream, req_id: req_id, method: method, urlString: url, headers: headers, ipc: self)
        } else {
            ipcRequest = IpcRequest.fromText(text: obj.body as? String ?? "", req_id: req_id, method: method, urlString: url, headers: headers)
        }
        
        self.postMessage(message: ipcRequest)
        return self.registerRedId(red_id: req_id).promise
    }
    
    func registerRedId(red_id: Int?) -> PromiseOut<IpcResponse> {
        
        var id = red_id
        if id == nil {
            id = self.allocReqId()
        }
        
        let response_po = PromiseOut<IpcResponse>()
        self.reqresMap[id!] = response_po
        self.initReqRes()
        return response_po
        
    }*/
    
    func onClose(cb: @escaping SimpleCallbcak) -> GenericsClosure<SimpleCallbcak> {
        return self.closeSignal.listen(cb)
    }
}

class initObj {
    
    var method: String?
    var body: Any?
    var headers: IpcHeaders?
}
