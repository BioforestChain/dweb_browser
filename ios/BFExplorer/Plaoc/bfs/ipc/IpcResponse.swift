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
    
    init(req_id: Int, statusCode: Int, headers: IpcHeaders, body: IpcBody) {
        self.req_id = req_id
        self.statusCode = statusCode
        self.headers = headers
        self.body = body
    }
    
    static func fromJson(req_id: Int, statusCode: Int = 200, jsonAble: [String:Any], headers: IpcHeaders, ipc: Ipc) -> IpcResponse {
        return fromText(req_id: req_id, statusCode: statusCode, text: ChangeTools.dicValueString(jsonAble)!, headers: headers, ipc: ipc)
    }
    
    static func fromText(req_id: Int, statusCode: Int = 200, text: String, headers: IpcHeaders, ipc: Ipc) -> IpcResponse {
        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: .init(text: text), ipc: ipc))
    }
    
    static func fromBinary(req_id: Int, statusCode: Int = 200, binary: Data, headers: IpcHeaders, ipc: Ipc) -> IpcResponse {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        
        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: .init(u8a: binary), ipc: ipc))
    }
    
    static func fromStream(req_id: Int, statusCode: Int = 200, stream: IpcBody.StreamData, headers: IpcHeaders, ipc: Ipc) -> IpcResponse {
        var headers = headers
        headers.set(key: "Content-Type", value: "application/octet-stream")
       
        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: .init(stream: stream), ipc: ipc))
    }
    
    static func fromResponse(req_id: Int, response: Response, ipc: Ipc) -> IpcResponse {
        if response.body.string != nil {
            return fromText(req_id: req_id, text: response.body.string!, headers: IpcHeaders(response.headers), ipc: ipc)
        } else if response.body.buffer != nil {
            var buffer = response.body.buffer!
            return fromBinary(req_id: req_id, binary: buffer.readData(length: buffer.readableBytes)!, headers: IpcHeaders(response.headers), ipc: ipc)
        } else if response.body.data != nil {
            return fromBinary(req_id: req_id, binary: response.body.data!, headers: IpcHeaders(response.headers), ipc: ipc)
        } else if response.body.count == -1 {
            var data = Data()
            _ = response.body.collect(on: HttpServer.app.eventLoopGroup.next()).map { bytebuffer in
                if var buffer = bytebuffer, buffer.readableBytes > 0 {
                    data.append(buffer.readData(length: buffer.readableBytes)!)
                }
            }
            
            return fromStream(req_id: req_id, stream: .init(stream: data), headers: IpcHeaders(response.headers), ipc: ipc)
        } else {
            return fromText(req_id: req_id, text: "", headers: IpcHeaders(response.headers), ipc: ipc)
        }
    }
    
    func toResponse() -> Response {
        var _body: Response.Body
        
        if body.body.text != nil {
            _body = .init(string: body.body.text!)
        } else if body.body.u8a != nil {
            _body = .init(data: body.body.u8a!)
        } else if body.body.stream != nil {
            _body = .init(stream: { writer in
                var stream = InputStream(data: self.body.body.stream!.stream)
                let bufferSize = 1024
                stream.open()
                
                while stream.hasBytesAvailable {
                    var data = Data()
                    var buffer = [UInt8](repeating: 0, count: bufferSize)
                    let bytesRead = stream.read(&buffer, maxLength: bufferSize)
                    if bytesRead < 0 {
                        stream.close()
                        _ = writer.write(.error("Error reading from stream" as! Error))
                    } else if bytesRead == 0 {
                        stream.close()
                        _ = writer.write(.end)
                    }
                    data.append(buffer, count: bytesRead)
                    var byteBuffer = ByteBuffer(data: data)
                    _ = writer.write(.buffer(byteBuffer))
                }
            })
        } else {
            fatalError("invalid body to response: \(body)")
        }
        
        return Response(status: HTTPResponseStatus(statusCode: statusCode), headers: headers.toHTTPHeaders(), body: _body)
    }
    
    lazy var ipcResMessage: IpcResMessage = IpcResMessage(req_id: req_id, statusCode: statusCode, headers: headers, metaBody: body.metaBody)
}

struct IpcResMessage: IpcMessage {
    var type: IPC_MESSAGE_TYPE = .response
    let req_id: Int
    let statusCode: Int
    let headers: IpcHeaders
    let metaBody: MetaBody
    
    func toIpcResponse(ipc: Ipc) -> IpcResponse {
        IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodyReceiver(metaBody: metaBody, ipc: ipc))
    }
}
