//
//  StreamAsRawData.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import UIKit

class StreamAsRawData {
    
    var stream_id: String
    var stream: InputStream
    var ipc: Ipc
    
    init(stream_id: String, stream: InputStream, ipc: Ipc) {
        self.stream_id = stream_id
        self.stream = stream
        self.ipc = ipc
    }
}
