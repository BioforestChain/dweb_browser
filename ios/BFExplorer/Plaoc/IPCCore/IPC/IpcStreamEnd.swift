//
//  IpcStreamEnd.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcStreamEnd: NSObject {

    let type = IPC_DATA_TYPE.STREAM_END
    private(set) var stream_id: String = ""
    
    init(stream_id: String) {
        super.init()
        self.stream_id = stream_id
    }
}
