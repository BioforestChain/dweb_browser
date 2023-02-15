//
//  IpcStreamPull.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcStreamPull: NSObject {

    let type = IPC_DATA_TYPE.STREAM_PULL
    private(set) var desiredSize: UInt8?
    
    init(stream_id: String, desiredSize: UInt8?) {
        super.init()
        if desiredSize == nil {
            self.desiredSize = 1
        } else if (isPurnFloat(value: "\(desiredSize!)")) {
            self.desiredSize = 1
        } else if (desiredSize! < 1) {
            self.desiredSize = 1
        }
        self.desiredSize = desiredSize
    }
    
    private func isPurnFloat(value: String) -> Bool {
        
        let scan = Scanner(string: value)
        var val: Float = 0
        return scan.scanFloat(&val) && scan.isAtEnd
    }
}
