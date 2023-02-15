//
//  IpcResponse.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import Network
import Alamofire

class IpcResponse: IpcBody {

    private let type = IPC_DATA_TYPE.RESPONSE
    private var ipcHeaders: [String: Any] = [:]
    private var headers: [String:String] = [:]
    
    init(req_id: Int, statusCode: Int, rawBody: RawData, headers: [String:String], ipc: Ipc?) {
        super.init(rawBody: rawBody, ipc: ipc)
        self.headers = headers
    }
    
    //TODO
    func asResponse(url: String?) {
        let body = self.bodyData()
        
        if body is [UInt8] {
            if self.headers["content-length"] == nil {
                self.headers["content-length"] = "\((body as! [UInt8]).count)"
            }
        }
       
        //TODO
        
        if url != nil {
            
        }
    }
    
    static func fromResponse(req_id: Int, response: HTTPURLResponse, ipc: Ipc) {
        
        var ipcResponse: IpcResponse?
        
    }
    
    static func fromJson(req_id: Int,statusCode: Int,jsonable: Any,headers: [String:String])  -> IpcResponse {
        
        let headerDict = ["Content-Type": "application/json"]
        
        let ipcResponse = self.fromText(req_id: req_id, statusCode: statusCode, text: ChangeTools.tempAnyToString(value:jsonable), headers: headers)
        return ipcResponse
    }
    
    static func fromText(req_id: Int,statusCode: Int,text: String,headers: [String:String])  -> IpcResponse {
        
        let headerDict = ["Content-Type": "text/plain"]
        
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: RawData(raw: .TEXT, content: text), headers: headerDict, ipc: nil)
        return ipcResponse
    }
    
    static func fromBinary(req_id: Int,statusCode: Int,binary: [UInt8],headers: [String:String],ipc: Ipc)  -> IpcResponse {
        
        var headerDict = ["Content-Type": "application/octet-stream"]
        headerDict["Content-Length"] = "\(binary.count)"
        
        let data = Data(bytes: binary, count: binary.count)
        let result = String(data: data, encoding: .utf8) ?? ""
        
        let rawBody = (ipc.support_message_pack ?? false) ? RawData(raw: .BINARY, content: result) : RawData(raw: .BASE64, content: encoding.simpleDecoder(data: binary, encoding: .base64) ?? "")
        
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: rawBody, headers: headerDict, ipc: nil)
        return ipcResponse
    }
    
    static func fromStream(req_id: Int,statusCode: Int,stream: Data,headers: [String:String],ipc: Ipc) -> IpcResponse {
        
        let headerDict = ["Content-Type": "application/octet-stream"]
        let stream_id = "res/\(req_id)/\(headerDict["content-length"] ?? "-")"
        let ipcResponse = IpcResponse(req_id: req_id, statusCode: statusCode, rawBody: RawData(raw: .BINARY_STREAM_ID, content: stream_id), headers: headerDict, ipc: ipc)
        //TODO
        //streamAsRawData(stream_id, stream, ipc);
        return ipcResponse
    }
}
