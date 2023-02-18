//
//  Ipc.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import PromiseKit

var ipc_uid_acc = 0

class Ipc: NSObject {

    var support_message_pack: Bool?
    var uid: Int {
        ipc_uid_acc = ipc_uid_acc + 1
        return ipc_uid_acc
    }
//    var remote: MicroModule?
    var role: IPC_ROLE?
    
    private var closed: Bool = false
    
    private var ipcMessage: OnIpcMessage?
    
    private var onIpcrequestMessage: OnIpcrequestMessage?
    
    var messageSignal: Signal<(IpcMessage,Ipc),Any>?//Signal<OnIpcMessage>?
    var onMessage: ((@escaping OnIpcMessage) -> () -> Bool)!
    
    private var closeSignal: Signal<(), Any>?
    private var onClose: ((@escaping (()) -> Any) -> () -> Bool)?
    
    private(set) var reqresMap: [Int: PromiseOut<IpcResponse>] = [:]
    private var req_id_acc = 0
    private var inited_req_res: Bool = false
    
    private var emptyResult: (() -> Bool)!
    
    override init() {
        super.init()
        messageSignal = Signal<(IpcMessage,Ipc),Any>.createSignal()
        onMessage = messageSignal!.listen
        
        
        closeSignal = Signal<(),Any>.createSignal()
        onClose = closeSignal?.listen
        
    }
    
    func postMessage(message: IpcMessage) {
        guard !closed else { return }
        doPostMessage(data: message)
    }
    
    func doPostMessage(data: IpcMessage) { }
    
    func getOnRequestListener() -> ((@escaping OnIpcrequestMessage) -> () -> Bool) {
        
        return onceCode
    }
    
    func onRequest(cb: @escaping OnIpcrequestMessage) -> Any {
      
        return self.getOnRequestListener()(cb)
    }
    
    func doClose() { }
    
    func closeAction() {
        guard !self.closed else { return }
        self.closed = true
        self.doClose()
        self.closeSignal?.emit(())
    }
    
    func allocReqId() -> Int {
        req_id_acc = req_id_acc + 1
        return req_id_acc
    }
    
    
    
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
            return self.emptyResult
        })()
    }
    
    func request(url: String, obj: initObj) -> Promise<IpcResponse>? {
        let req_id = self.allocReqId()
        let method = obj.method ?? "GET"
        let headers: [String:String] = [:]  //TODO
        let ipcRequest: IpcRequest
        
        if obj.body is [UInt8] {
            ipcRequest = IpcRequest.fromBinary(binary: obj.body as! [UInt8], req_id: req_id, method: method, url: url, headers: headers, ipc: self)
        } else if obj.body is Data {
            let stream = InputStream(data: obj.body as! Data)
            ipcRequest = IpcRequest.fromStream(stream: stream, req_id: req_id, method: method, url: url, headers: headers, ipc: self)
        } else {
            ipcRequest = IpcRequest.fromText(text: obj.body as? String ?? "", req_id: req_id, method: method, url: url, headers: headers)
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
        
    }
    
    lazy var onceCode: ((@escaping OnIpcrequestMessage) -> () -> Bool) = {
            
        let signal = Signal<(IpcRequest,Ipc),Any>.createSignal()
        let function = self.onMessage({ (request, ipc) in
       
            if let request = request as? IpcRequest {
                signal.emit((request, ipc))
            }
            return self.emptyResult
        })()
        
        return signal.listen
        
    }()
}

class initObj {
    
    var method: String?
    var body: Any?
    var headers: [String: Any]?
}
