//
//  IpcResponse.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import Vapor


class IpcResponse: IpcBody {

    var req_id: Int = 0
    private let type = IPC_DATA_TYPE.RESPONSE
    private var headers: IpcHeaders!
    private var statusCode: Int = 0
    
    init(req_id: Int, statusCode: Int, rawBody: RawData, headers: IpcHeaders, ipc: Ipc?) {
        super.init(rawBody: rawBody, ipc: ipc)
        self.headers = headers
        self.req_id = req_id
        self.statusCode = statusCode
    }
    
    func getIpcHeaders() -> [String:String] {
        return self.headers.headerDict
    }
    
    func asResponse(url: String?) -> Response {
        let body = self.bodyData()
        
        var headers = HTTPHeaders()
        if body is [UInt8] {
            for (key, value) in self.headers.headerDict {
                headers.add(name: key, value: value)
            }
            if self.headers.getValue(forKey: "content-length") == nil {
                headers.add(name: "content-length", value: "\((body as! [UInt8]).count)")
            }
        }
        var responseBody = Response.Body()
        if body is String {
            responseBody = Response.Body.init(string: body as? String ?? "")
        } else if body is [UInt8] {
            let bodyData = body as! [UInt8]
            let data = Data(bytes: bodyData, count: bodyData.count)
            responseBody = Response.Body.init(data: data)
        } else if body is InputStream {
            
            let data = IpcResponse.fetchStreamData(stream: body as! InputStream)
            responseBody = Response.Body.init(data: data)
        }
        
        let response = Response(status: HTTPResponseStatus(statusCode: self.statusCode), headers: headers, body: responseBody)
        return response
    }
    
    static func fetchStreamData(stream: InputStream) -> Data {
        
        stream.open()
        defer {
            stream.close()
        }

        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        
        var result: Data = Data()
        while stream.hasBytesAvailable {
            let length = stream.read(buffer, maxLength: bufferSize)
            let data = Data(bytes: buffer, count: length)
            result.append(data)
        }
        return result
    }
    
    static func fromResponse(req_id: Int, response: Response, ipc: Ipc) -> IpcResponse? {
        
        var ipcResponse: IpcResponse?
        guard response.body.data != nil else { return ipcResponse }
        if response.body.count > 0 {
            let stream = InputStream(data: response.body.data!)
            ipcResponse = self.fromStream(req_id: req_id, statusCode: Int(response.status.code), stream: stream, headers: IpcHeaders(content: response.headers.description), ipc: ipc)
        } else {
            ipcResponse = self.fromBinary(req_id: req_id, statusCode: Int(response.status.code), binary: [UInt8](response.body.data!), headers: IpcHeaders(content: response.headers.description), ipc: ipc)
        }
        return ipcResponse
    }
    
    static func fromJson(req_id: Int,statusCode: Int,jsonable: Any,headers: IpcHeaders)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/json")
        let ipcResponse = self.fromText(req_id: req_id, statusCode: statusCode, text: ChangeTools.tempAnyToString(value:jsonable), headers: headers)
        return ipcResponse
    }
    
    static func fromText(req_id: Int,statusCode: Int,text: String,headers: IpcHeaders)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "text/plain")
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: RawData(raw: .TEXT, content: text), headers: headers, ipc: nil)
        return ipcResponse
    }
    
    static func fromBinary(req_id: Int,statusCode: Int,binary: [UInt8],headers: IpcHeaders,ipc: Ipc)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        
        let data = Data(bytes: binary, count: binary.count)
        let result = String(data: data, encoding: .utf8) ?? ""
        
        let rawBody = (ipc.support_message_pack ?? false) ? RawData(raw: .BINARY, content: result) : RawData(raw: .BASE64, content: encoding.simpleDecoder(data: binary, encoding: .base64) ?? "")
        
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: rawBody, headers: headers, ipc: nil)
        return ipcResponse
    }
    
    static func fromStream(req_id: Int,statusCode: Int,stream: InputStream,headers: IpcHeaders,ipc: Ipc) -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        let stream_id = "res/\(req_id)/\(headers.getValue(forKey: "content-length") ?? "-")"
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: RawData(raw: .BINARY_STREAM_ID, content: stream_id), headers: headers, ipc: ipc)
        
        streamAsRawData.streamAsRawData(streamId: stream_id, stream: stream, ipc: ipc)
        return ipcResponse
    }
}

extension IpcResponse: IpcMessage {}
