//
//  IpcRequest.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation
import Vapor

final class IpcRequest {
    var type: IPC_DATA_TYPE = .request
    var req_id: Int
    var method: IpcMethod
    var url: String
    var headers: IpcHeaders
    var body: IpcBody
    
    init(req_id: Int, url: String, method: IpcMethod, headers: IpcHeaders, body: IpcBody) {
        self.req_id = req_id
        self.method = method
        self.url = url
        self.headers = headers
        self.body = body
    }
    
    lazy var uri: URI? = {
        return URI(string: url)
    }()
    
    static func fromText(
        req_id: Int,
        url: String,
        method: IpcMethod = .GET,
        headers: IpcHeaders,
        text: String,
        ipc: Ipc
    ) -> IpcRequest {
        return IpcRequest(req_id: req_id, url: url, method: method, headers: headers, body: IpcBodySender(body: .init(text: text), ipc: ipc))
    }
    
    static func fromBinary(
        req_id: Int,
        method: IpcMethod,
        url: String,
        headers: IpcHeaders,
        binary: Data,
        ipc: Ipc
    ) -> IpcRequest {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        return IpcRequest(req_id: req_id, url: url, method: method, headers: headers, body: IpcBodySender(body: .init(u8a: binary), ipc: ipc))
    }
    
    static func fromStream(
        req_id: Int,
        method: IpcMethod,
        url: String,
        headers: IpcHeaders,
        stream: IpcBody.StreamData,
        ipc: Ipc,
        size: Int64? = nil
    ) -> IpcRequest {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
        
        if size != nil {
            headers.set(key: "Content-Length", value: "\(size!)")
        }
        
        return IpcRequest(req_id: req_id, url: url, method: method, headers: headers, body: IpcBodySender(body: .init(stream: stream), ipc: ipc))
    }
    
    static func fromRequest(req_id: Int, request: Request, ipc: Ipc) -> IpcRequest {
        if request.body.data != nil {
            return fromBinary(req_id: req_id, method: IpcMethod.from(vaporMethod: request.method), url: request.url.string, headers: IpcHeaders(request.headers), binary: Data(buffer: request.body.data!), ipc: ipc)
        } else if request.method == .POST || request.method == .PUT || request.method == .PATCH {
            var ipc_req_body_stream: Data = Data()
            var sequential = request.eventLoop.makeSucceededFuture(())
            
            request.body.drain {
                switch $0 {
                case .buffer(var buffer):
                    if buffer.readableBytes > 0 {
                        ipc_req_body_stream.append(buffer.readData(length: buffer.readableBytes)!)
                    }
                    
                    return sequential
                case .error(_):
                    return sequential
                case .end:
                    return sequential
                }
            }
            
            return fromStream(req_id: req_id, method: IpcMethod.from(vaporMethod: request.method), url: request.url.string, headers: IpcHeaders(request.headers), stream: .init(stream: ipc_req_body_stream), ipc: ipc, size: Int64(ipc_req_body_stream.count))
        } else {
            return fromText(req_id: req_id, url: request.url.string, headers: IpcHeaders(request.headers), text: request.body.string ?? "", ipc: ipc)
        }
    }
    
    func toRequest() -> Request {
        if method == .GET || method == .HEAD {
            return Request.new(method: HTTPMethod(rawValue: method.rawValue), url: self.url)
        }
        
        var buffer: ByteBuffer
        
        if body.body.text != nil {
            buffer = .init(string: body.body.text!)
        } else if body.body.u8a != nil {
            buffer = .init(data: body.body.u8a!)
        } else if body.body.stream != nil {
            buffer = .init(data: body.body.stream!.stream)
        } else {
            fatalError("invalid body to request: \(body)")
        }
        
        return Request.new(method: HTTPMethod(rawValue: method.rawValue), url: self.url, collectedBody: buffer)
    }
    
    lazy var ipcReqMessage: IpcReqMessage = IpcReqMessage(req_id: req_id, method: method, url: url, headers: headers, metaBody: body.metaBody)
}

struct IpcReqMessage: IpcMessage {
    var type: IPC_DATA_TYPE = .request
    let req_id: Int
    let method: IpcMethod
    let url: String
    let headers: IpcHeaders
    let metaBody: MetaBody
    
    func toIpcRequest() -> IpcRequest {
        return IpcRequest(req_id: req_id, url: url, method: method, headers: headers, body: .init(metaBody: metaBody, body: nil))
    }
}
