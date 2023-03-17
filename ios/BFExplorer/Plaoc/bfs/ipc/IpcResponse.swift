//
//  IpcResponse.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation
import Vapor

final class IpcResponse {
    var type: IPC_MESSAGE_TYPE = .response
    var req_id: Int
    var statusCode: Int
    var headers: IpcHeaders
    var body: IpcBody
    var ipc: Ipc
    
    init(req_id: Int, statusCode: Int, headers: IpcHeaders, body: IpcBody, ipc: Ipc) {
        self.req_id = req_id
        self.statusCode = statusCode
        self.headers = headers
        self.body = body
        self.ipc = ipc
    }
    
    static func fromJson(
        req_id: Int,
        statusCode: Int = 200,
        jsonAble: [String:Any],
        headers: IpcHeaders = .init([:]),
        ipc: Ipc
    ) -> IpcResponse {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/json")
        
        return fromText(
            req_id: req_id,
            statusCode: statusCode,
            text: ChangeTools.dicValueString(jsonAble)!,
            headers: headers, ipc: ipc)
    }
    
    static func fromText(
        req_id: Int,
        statusCode: Int = 200,
        text: String,
        headers: IpcHeaders = .init([:]),
        ipc: Ipc
    ) -> IpcResponse {
        return IpcResponse(
            req_id: req_id,
            statusCode: statusCode,
            headers: headers,
            body: IpcBodySender.from(raw: .init(key: text), ipc: ipc),
            ipc: ipc)
    }
    
    static func fromBinary(
        req_id: Int,
        statusCode: Int = 200,
        binary: Data,
        headers: IpcHeaders = .init([:]),
        ipc: Ipc
    ) -> IpcResponse {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        
        return IpcResponse(
            req_id: req_id,
            statusCode: statusCode,
            headers: headers,
            body: IpcBodySender.from(raw: .init(key: binary), ipc: ipc),
            ipc: ipc)
    }
    
    static func fromStream(
        req_id: Int,
        statusCode: Int = 200,
        stream: InputStream,
        headers: IpcHeaders,
        ipc: Ipc
    ) -> IpcResponse {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
       
        return IpcResponse(
            req_id: req_id,
            statusCode: statusCode,
            headers: headers,
            body: IpcBodySender.from(raw: .init(key: stream), ipc: ipc),
            ipc: ipc)
    }
    
    static func fromResponse(req_id: Int, response: Response, ipc: Ipc) -> IpcResponse {
        if response.body.string != nil {
            return fromText(
                req_id: req_id,
                text: response.body.string!,
                headers: IpcHeaders(response.headers),
                ipc: ipc)
        } else if response.body.buffer != nil {
            return fromBinary(
                req_id: req_id,
                binary: Data(response.body.buffer!.readableBytesView),
                headers: IpcHeaders(response.headers),
                ipc: ipc)
        } else if response.body.data != nil {
            return fromBinary(
                req_id: req_id,
                binary: response.body.data!,
                headers: IpcHeaders(response.headers),
                ipc: ipc)
        } else if response.body.count == -1 {
            var buffer = try? response.body.collect(on: HttpServer.app.eventLoopGroup.next()).wait()
            
            if buffer!.readableBytes > 0 {
                return fromStream(
                    req_id: req_id,
                    stream: .init(data: Data(buffer!.readableBytesView)),
                    headers: IpcHeaders(response.headers),
                    ipc: ipc)
            } else {
                return fromText(
                    req_id: req_id,
                    text: "",
                    headers: IpcHeaders(response.headers),
                    ipc: ipc)
            }
        } else {
            return fromText(
                req_id: req_id,
                text: "",
                headers: IpcHeaders(response.headers),
                ipc: ipc)
        }
    }
    
    func toResponse() -> Response {
        var _body: Response.Body
        
        if let resBody = body.raw as? String {
            _body = .init(string: resBody)
        } else if let resBody = body.raw as? Data {
            _body = .init(data: resBody)
        } else if let resBody = body.raw as? ReadableStream {
            _body = .init(stream: { $0.responseStreamWriter(stream: resBody) })
        } else {
            fatalError("invalid body to response: \(body)")
        }
        
        return Response(
            status: HTTPResponseStatus(statusCode: statusCode),
            headers: headers.toHTTPHeaders(),
            body: _body)
    }
    
    lazy var ipcResMessage: IpcResMessage = IpcResMessage(
        req_id: req_id, statusCode: statusCode, headers: headers, metaBody: body.metaBody
    )
}

struct IpcResMessage: IpcMessage {
    var type: IPC_MESSAGE_TYPE = .response
    let req_id: Int
    let statusCode: Int
    let headers: IpcHeaders
    let metaBody: MetaBody
    
    func toIpcResponse(ipc: Ipc) -> IpcResponse {
        IpcResponse(
            req_id: req_id,
            statusCode: statusCode,
            headers: headers,
            body: IpcBodySender(raw: metaBody.data.string ?? metaBody.data.data ?? "", ipc: ipc),
            ipc: ipc)
    }
}
