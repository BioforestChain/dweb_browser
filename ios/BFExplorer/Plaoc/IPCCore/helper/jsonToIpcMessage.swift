//
//  jsonToIpcMessage.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

import UIKit
import HandyJSON

class jsonToIpcMessage: NSObject {

    
    static func jsonToIpcMessage(data: String, ipc: Ipc) -> Any? {
        guard data != "close" else { return data }
        
        if let request = JSONDeserializer<IpcRequest>.deserializeFrom(json: data) {
//            let req = IpcRequest(req_id: request.req_id, method: request.method, urlString: request.urlString, rawBody: request., headers: <#T##IpcHeaders#>, ipc: <#T##Ipc?#>)
        }
        
        return data
    }
}
