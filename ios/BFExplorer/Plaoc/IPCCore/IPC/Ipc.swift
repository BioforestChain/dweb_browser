//
//  Ipc.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

var ipc_uid_acc = 0

class Ipc: NSObject {

    var support_message_pack: Bool?
    var uid: Int {
        ipc_uid_acc = ipc_uid_acc + 1
        return ipc_uid_acc
    }
    var remote: MicroModule?
    var role: IPC_ROLE?
    
    private var closed: Bool = false
    
    private var ipcMessage: OnIpcMessage?
    
    private var onIpcrequestMessage: OnIpcrequestMessage?
    
    var messageSignal: Signal?
    
    var onMessage: Signal?
    
    private var closeSignal: Signal?
    private var onClose: Signal?
    
//    private(set) var reqresMap: [Int: ]
    private var req_id_acc = 0
    private var inited_req_res: Bool = false
    
    override init() {
        super.init()
        messageSignal = Signal.createSignal(callback: ipcMessage)
        //TODO  closeSignal  onClose 初始化
    }
    
    func postMessage(message: IpcMessage) {
        guard !closed else { return }
        doPostMessage(data: message)
    }
    
    func doPostMessage(data: IpcMessage) { }
    
    func getOnRequestListener() {
        
        _ = onceCode
    }
    
    func onRequest(cb: OnIpcrequestMessage) {
        
    }
    
    func doClose() { }
    
    func closeAction() {
        guard !self.closed else { return }
        self.doClose()
        self.closeSignal?.emit(message: nil, nil)
    }
    
    func allocReqId() -> Int {
        req_id_acc = req_id_acc + 1
        return req_id_acc
    }
    
    private func initReqRes() {
        guard !inited_req_res else { return }
        self.inited_req_res = true
        
    }
    
    func request(url: String, obj: initObj) {
        let req_id = self.allocReqId()
        let method = obj.method ?? "GET"
        let headers: [String:String] = [:]  //TODO
        let ipcRequest: IpcRequest
        
        if obj.body is [UInt8] {
            ipcRequest = IpcRequest.fromBinary(binary: obj.body as! [UInt8], req_id: req_id, method: method, url: url, headers: headers, ipc: self)
        } else if obj.body is Data {
            ipcRequest = IpcRequest.fromStream(stream: obj.body as! Data, req_id: req_id, method: method, url: url, headers: headers, ipc: self)
        } else {
            ipcRequest = IpcRequest.fromText(text: obj.body as? String ?? "", req_id: req_id, method: method, url: url, headers: headers)
        }
        
//        self.postMessage(message: ipcRequest)
    }
    
    lazy var onceCode: Void = {
        //TODO
        let signal = Signal.createSignal(callback: onIpcrequestMessage)
    }()
}

class initObj {
    
    var method: String?
    var body: Any?
    var headers: [String: Any]?
}
