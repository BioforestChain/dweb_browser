//
//  IpcReqMessage.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import Foundation

struct IpcReqMessage: IpcMessage {
  
    var type: IPC_DATA_TYPE = .REQUEST
    var req_id: Int = 0
    var method: String = ""
    var urlString: String = ""
    var headers: [String: String] = [:]
    var metaBody: MetaBody?
    
    init() {
        
    }
    
    init(req_id: Int, method: String, urlString: String, headers: [String: String], metaBody: MetaBody?) {
        self.req_id = req_id
        self.method = method
        self.urlString = urlString
        self.headers = headers
        self.metaBody = metaBody
    }
}
