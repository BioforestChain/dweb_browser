//
//  IpcRequest.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import AsyncHTTPClient
import Alamofire
import SwiftUI

class IpcRequest {

    private let type = IPC_DATA_TYPE.REQUEST
    private var parsed_url: URL?
    var req_id: Int = 0
    private var method: String = ""
    private var headers: IpcHeaders?
    private var urlString: String = ""
    private var body: IpcBody?
    var url: URL?
    
    required init() {
        
    }
    
    init(req_id: Int, method: String, urlString: String, headers: IpcHeaders, body: IpcBody?) {
        
        self.urlString = urlString
        self.req_id = req_id
        self.method = method
        self.headers = headers
        self.body = body
        url = URL(string: urlString)
    }
    
    static func fromText(text: String,req_id: Int,method: String,urlString: String,headers: IpcHeaders, ipc: Ipc) -> IpcRequest {
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender(body: text, ipc: ipc))
    }
    
    static func fromBinary(binary: [UInt8],req_id: Int,method: String,urlString: String,headers:IpcHeaders, ipc: Ipc) -> IpcRequest {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        headers.set(key: "Content-Length", value: "\(binary.count)")
        
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender(body: binary, ipc: ipc))
    }
    
    static func fromStream(stream: InputStream,req_id: Int,method: String,urlString: String,headers:IpcHeaders, ipc: Ipc, size: Int64?) -> IpcRequest {
        
        headers.set(key: "Content-Type", value: "application/octet-stream")
        if size != nil {
            headers.set(key: "Content-Length", value: "\(size!)")
        }
        return IpcRequest(req_id: req_id, method: method, urlString: urlString, headers: headers, body: IpcBodySender(body: stream, ipc: ipc))
    }
    
    static func fromRequest(req_id: Int,request: URLRequest, ipc: Ipc) -> IpcRequest {
        
        let header = IpcHeaders()
        for (key,value) in request.allHTTPHeaderFields! {
            header.set(key: key, value: value)
        }
        
        if request.httpBody == nil {
            return IpcRequest(req_id: req_id, method: request.httpMethod ?? "", urlString: request.url?.absoluteString ?? "", headers: header, body: IpcBodySender(body: request.httpBodyStream, ipc: ipc))
        } else if request.httpBody?.count == 0 {
            return IpcRequest(req_id: req_id, method: request.httpMethod ?? "", urlString: request.url?.absoluteString ?? "", headers: header, body: IpcBodySender(body: "", ipc: ipc))
        }
        return IpcRequest(req_id: req_id, method: request.httpMethod ?? "", urlString: request.url?.absoluteString ?? "", headers: header, body: IpcBodySender(body: [UInt8](request.httpBody!), ipc: ipc))
    }
    
    func asRequest() -> URLRequest? {
        guard let url = URL(string: urlString) else { return nil }
        let headerDict = headers != nil ? headers!.headerDict : nil
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.allHTTPHeaderFields = headerDict
        if let content = body?.body as? String {
            request.httpBody = content.data(using: .utf8)
        } else if let bytes = body?.body as? [UInt8] {
            request.httpBodyStream = InputStream(data: Data(bytes: bytes, count: bytes.count))
        } else if let stream = body?.body as? InputStream {
            request.httpBodyStream = stream
        }
        return request
    }
}

extension IpcRequest: IpcMessage { }
