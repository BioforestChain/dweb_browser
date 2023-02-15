//
//  IpcStreamAbort.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcStreamAbort: NSObject {

    let type = IPC_DATA_TYPE.STREAM_ABORT
    
    init(stream_id: String) {
        super.init()
        
    }
}
