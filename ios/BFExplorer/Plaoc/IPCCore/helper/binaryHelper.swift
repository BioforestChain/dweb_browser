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
    
    static func dataToBinary(data: Any, encoding: IPC_DATA_ENCODING) -> [UInt8]? {
        
        switch encoding {
        case .BINARY:
            return data as? [UInt8]
        case .BASE64:
            return (data as? String)?.fromBase64()
        case .UTF8:
            return (data as? String)?.fromUtf8()
        default:
            print("invalid metaBody.type :\(String(describing: encoding))")
            return nil
        }
    }
    
    static func dataToText(data: Any, encoding: IPC_DATA_ENCODING) -> String? {
        switch encoding {
        case .BINARY:
            return (data as? [UInt8])?.toUtf8()
        case .BASE64:
            return (data as? String)?.fromBase64()?.toUtf8()
        case .UTF8:
            return data as? String
        default:
            print("invalid metaBody.type :\(String(describing: encoding))")
            return nil
        }
    }
}
