//
//  IpcRequest.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation
import Vapor

final class IpcRequest {
    var type: IPC_MESSAGE_TYPE = .request
    var req_id: Int
    var method: IpcMethod
    var url: String
    var headers: IpcHeaders
    var body: IpcBody
    var ipc: Ipc
    
    init(req_id: Int, url: String, method: IpcMethod, headers: IpcHeaders, body: IpcBody, ipc: Ipc) {
        self.req_id = req_id
        self.method = method
        self.url = url
        self.headers = headers
        self.body = body
        self.ipc = ipc
        
        if let body = body as? IpcBodySender {
            IpcBodySender.IPC.usableByIpc(ipc: ipc, ipcBody: body)
        }
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
        return IpcRequest(
            req_id: req_id,
            url: url,
            method: method,
            headers: headers,
            body: IpcBodySender(raw: text, ipc: ipc),
            ipc: ipc)
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
        return IpcRequest(
            req_id: req_id,
            url: url,
            method: method,
            headers: headers,
            body: IpcBodySender(raw: binary, ipc: ipc),
            ipc: ipc)
    }
    
    static func fromStream(
        req_id: Int,
        method: IpcMethod,
        url: String,
        headers: IpcHeaders,
        stream: InputStream,
        ipc: Ipc,
        size: Int64? = nil
    ) -> IpcRequest {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
        
        if size != nil {
            headers.set(key: "Content-Length", value: "\(size!)")
        }
        
        return IpcRequest(
            req_id: req_id,
            url: url,
            method: method,
            headers: headers,
            body: IpcBodySender(raw: stream, ipc: ipc),
            ipc: ipc)
    }
    
    static func fromRequest(req_id: Int, request: Request, ipc: Ipc) -> IpcRequest {
        if request.body.data != nil {
            return fromBinary(
                req_id: req_id,
                method: IpcMethod.from(vaporMethod: request.method),
                url: request.url.string,
                headers: IpcHeaders(request.headers),
                binary: Data(buffer: request.body.data!),
                ipc: ipc)
        } else if request.method == .POST || request.method == .PUT || request.method == .PATCH {
            var buffer = try? request.body.collect().wait()
            
            if buffer != nil {
                let ipc_req_body_stream = Data(buffer!.readableBytesView)
                
                return fromStream(
                    req_id: req_id,
                    method: IpcMethod.from(vaporMethod: request.method),
                    url: request.url.string, headers: IpcHeaders(request.headers),
                    stream: InputStream(data: ipc_req_body_stream),
                    ipc: ipc,
                    size: Int64(ipc_req_body_stream.count))
            } else {
                return fromText(
                    req_id: req_id,
                    url: request.url.string,
                    method: IpcMethod.from(vaporMethod: request.method),
                    headers: IpcHeaders(request.headers),
                    text: "",
                    ipc: ipc)
            }
        } else {
            return fromText(
                req_id: req_id,
                url: request.url.string,
                headers: IpcHeaders(request.headers),
                text: request.body.string ?? "",
                ipc: ipc)
        }
    }
    
    func toRequest() -> Request {
        if method == .GET || method == .HEAD {
            return Request.new(method: HTTPMethod(rawValue: method.rawValue), url: self.url)
        }
        
        var buffer: ByteBuffer
        
        if let resBody = body.raw as? String {
            buffer = .init(string: resBody)
        } else if let resBody = body.raw as? Data {
            buffer = .init(data: resBody)
        } else if let resBody = body.raw as? InputStream {
            buffer = .init(data: Data(reading: resBody))
        } else {
            fatalError("invalid body to request: \(body)")
        }
        
//        if body.bodyHub.text != nil {
//            buffer = .init(string: body.bodyHub.text!)
//        } else if body.bodyHub.u8a != nil {
//            buffer = .init(data: body.bodyHub.u8a!)
//        } else if body.bodyHub.stream != nil {
//            buffer = .init(data: Data(reading: body.bodyHub.stream!))
//        } else {
//            fatalError("invalid body to request: \(body)")
//        }
        
        return Request.new(method: HTTPMethod(rawValue: method.rawValue), url: self.url, collectedBody: buffer)
    }
    
    lazy var ipcReqMessage: IpcReqMessage = IpcReqMessage(req_id: req_id, method: method, url: url, headers: headers, metaBody: body.metaBody)
}

struct IpcReqMessage: IpcMessage {
    var type: IPC_MESSAGE_TYPE = .request
    let req_id: Int
    let method: IpcMethod
    let url: String
    let headers: IpcHeaders
    let metaBody: MetaBody
    
    func toIpcRequest(ipc: Ipc) -> IpcRequest {
        return IpcRequest(
            req_id: req_id,
            url: url,
            method: method,
            headers: headers,
            body: IpcBodySender(raw: metaBody.data.string ?? metaBody.data.data ?? "", ipc: ipc),
            ipc: ipc)
    }
}
