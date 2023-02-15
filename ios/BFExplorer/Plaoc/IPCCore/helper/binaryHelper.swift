//
//  binaryHelper.swift
//  IPC
//
//  Created by ui03 on 2023/2/14.
//

import UIKit

class binaryHelper: NSObject {
    
    
    static func u8aConcat(binaryList: [[UInt8]]) -> [UInt8] {
        var totalLength = 0
        for binary in binaryList {
            totalLength += binary.count
        }
        var result: [UInt8] = []
        for binary in binaryList {
            result += binary
        }
        return result
    }
}
