//
//  IpcRequest.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import Vapor

class IpcRequest {

    var type: IPC_MESSAGE_TYPE = .REQUEST
    var parsed_url: URL?
    var req_id: Int = 0
    var method: String = ""
    var headers: IpcHeaders?
    var urlString: String = ""
    var body: IpcBody?
    var ipc: Ipc?
    var url: URL?
    
    required init() {
        
    }
    
    init(req_id: Int, method: String, urlString: String, headers: IpcHeaders, body: IpcBody?, ipc: Ipc?) {
        
        self.ipc = ipc
        self.urlString = urlString
        self.req_id = req_id
        self.method = method
        self.headers = headers
        self.body = body
        url = URL(string: urlString)
        
        if let sender = body as? IpcBodySender, ipc != nil {
            IpcBodySender().IPCsender(ipc: ipc!, ipcBody: sender)
        }
    }
    
    static func fromText(text: String,req_id: Int,method: String = "GET",urlString: String,headers: IpcHeaders, ipc: Ipc) -> IpcRequest {
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender.from(raw: text, ipc: ipc), ipc: ipc)
    }
    
    static func fromBinary(binary: [UInt8],req_id: Int,method: String,urlString: String,headers:IpcHeaders, ipc: Ipc) -> IpcRequest {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender.from(raw: binary, ipc: ipc), ipc: ipc)
    }
    
    static func fromStream(stream: InputStream,req_id: Int,method: String,urlString: String,headers:IpcHeaders, ipc: Ipc, size: Int64?) -> IpcRequest {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        if size != nil {
            headers.set(key: "Content-Length", value: "\(size!)")
        }
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender.from(raw: stream, ipc: ipc), ipc: ipc)
    }
    
    static func fromRequest(req_id: Int,request: Request, ipc: Ipc) -> IpcRequest {
        
        let header = IpcHeaders(content: request.headers.description)
        
        if request.method == .GET || request.method == .HEAD {
            return IpcRequest(req_id: req_id, method: request.method.rawValue, urlString: request.url.string, headers: header, body: IpcBodySender.from(raw: "", ipc: ipc), ipc: ipc)
        }
        
        if request.body.data != nil {
            return self.fromBinary(binary: [UInt8](Data(buffer: request.body.data!)), req_id: req_id, method: request.method.rawValue, urlString: request.url.string, headers: header, ipc: ipc)
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
            
            return self.fromStream(stream: InputStream(data: ipc_req_body_stream), req_id: req_id, method: request.method.rawValue, urlString: request.url.string, headers: header, ipc: ipc, size: Int64(ipc_req_body_stream.count))
        }else {
            return self.fromText(text: request.body.string ?? "", req_id: req_id, method: request.method.rawValue, urlString: request.url.string, headers: header, ipc: ipc)
        }
    }
    
    func toRequest() -> Request {
        
        if method == "GET" || method == "HEAD" {
            return Request.new(method: HTTPMethod(rawValue: method), url: self.urlString)
        }
        
        var buffer: ByteBuffer
        
        if let content = body?.raw as? String {
            buffer = .init(string: content)
        } else if let bytes = body?.raw as? [UInt8] {
            buffer = .init(data: Data(bytes))
        } else if let stream = body?.raw as? InputStream {
            buffer = .init(data: Data(stream.readByteArray()))
        } else {
            fatalError("invalid body to request: \(body)")
        }
        return Request.new(method: HTTPMethod(rawValue: method), url: self.urlString, collectedBody: buffer)
        
    }
    
    func toString() -> String {
        return "#IpcRequest/\(method)/\(urlString)"
    }
    
    lazy var ipcReqMessage: IpcReqMessage = {
        return IpcReqMessage(req_id: req_id, method: method, urlString: urlString, headers: headers?.headerDict ?? [:], metaBody: body?.metaBody)
    }()
}

extension IpcRequest: IpcMessage { }
