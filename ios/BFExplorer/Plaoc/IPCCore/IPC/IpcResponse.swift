//
//  IpcResponse.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcResponse: IpcBody {

    private let type = IPC_DATA_TYPE.RESPONSE
    
    init(req_id: Int, statusCode: Int, rawBody: RawData, headers: [String:String], ipc: Ipc?) {
        super.init(rawBody: rawBody, ipc: ipc)
    }
    
    
}
