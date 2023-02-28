//
//  IpcResponse.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import Vapor


class IpcResponse {

    var req_id: Int = 0
    private let type = IPC_DATA_TYPE.RESPONSE
    private var headers: IpcHeaders!
    private var statusCode: Int = 0
    private var body: IpcBody?
    
    required init() {
        
    }
    
    init(req_id: Int, statusCode: Int, headers: IpcHeaders, body: IpcBody) {
        
        self.headers = headers
        self.req_id = req_id
        self.statusCode = statusCode
        self.body = body
    }
    
    lazy var ipcResMessage: IpcResMessage = {
        let message = IpcResMessage(req_id: self.req_id, statusCode: self.statusCode, headers: self.headers, metaBody: self.body?.metaBody)
        return message
    }()
    
    func getIpcHeaders() -> [String:String] {
        return self.headers.headerDict
    }
    
    func asResponse() -> Response {
        
        var headers = HTTPHeaders()
        for (key, value) in self.headers.headerDict {
            headers.add(name: key, value: value)
        }
     
        var responseBody = Response.Body()
        if let content = body?.body as? String {
            responseBody = Response.Body.init(string: content)
        } else if let tytes = body?.body as? [UInt8] {
            let data = Data(bytes: tytes, count: tytes.count)
            responseBody = Response.Body.init(data: data)
        } else if let stream = body?.body as? InputStream {
            let data = IpcResponse.fetchStreamData(stream: stream)
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
        
        guard response.body.data != nil else { return nil }
        let ipcResponse: IpcResponse?
        
        if response.body.count == -1 {
            let stream = InputStream(data: response.body.data!)
            return IpcResponse(req_id: req_id, statusCode: Int(response.status.code), headers: IpcHeaders(content: response.headers.description), body: IpcBodySender(body: stream, ipc: ipc))
        } else if response.body.count > 0 {
            return IpcResponse(req_id: req_id, statusCode: Int(response.status.code), headers: IpcHeaders(content: response.headers.description), body: IpcBodySender(body: [UInt8](response.body.data!), ipc: ipc))
        } else {
            return IpcResponse(req_id: req_id, statusCode: Int(response.status.code), headers: IpcHeaders(content: response.headers.description), body: IpcBodySender(body: "", ipc: ipc))
        }
    }
    
    static func fromJson(req_id: Int,statusCode: Int = 200,jsonable: Any,headers: IpcHeaders = IpcHeaders(),ipc: Ipc)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/json")
        let ipcResponse = self.fromText(req_id: req_id, statusCode: statusCode, text: ChangeTools.tempAnyToString(value:jsonable), headers: headers, ipc: ipc)
        return ipcResponse
    }
    
    static func fromText(req_id: Int,statusCode: Int = 200,text: String,headers: IpcHeaders = IpcHeaders(),ipc: Ipc)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "text/plain")
        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: text, ipc: ipc))
    }
    
    static func fromBinary(req_id: Int,statusCode: Int = 200,binary: [UInt8],headers: IpcHeaders,ipc: Ipc)  -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")

        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: binary, ipc: ipc))
    }
    
    static func fromStream(req_id: Int,statusCode: Int = 200,stream: InputStream,headers: IpcHeaders = IpcHeaders(),ipc: Ipc) -> IpcResponse {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        return IpcResponse(req_id: req_id, statusCode: statusCode, headers: headers, body: IpcBodySender(body: stream, ipc: ipc))
    }
}

extension IpcResponse: IpcMessage {}



struct IpcResMessage: IpcMessage {
    
    var req_id: Int?
    var statusCode: Int?
    var headers: IpcHeaders?
    var metaBody: MetaBody?
    
    init() {
        
    }
    
    init(req_id: Int, statusCode: Int, headers: IpcHeaders, metaBody: MetaBody?) {
        self.req_id = req_id
        self.statusCode = statusCode
        self.headers = headers
        self.metaBody = metaBody
    }
}
