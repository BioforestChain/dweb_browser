//
//  encoding.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

enum SimpleEncoding: String {
    case utf8 = "utf8"
    case base64 = "base64"
}

class encoding: NSObject {

    static func simpleEncoder(data: String, encoding: SimpleEncoding) -> [UInt8] {
        
        var binary: [UInt8] = []
        if encoding == .base64 {
            if let byteCharacters = data.base64Decoding() {
                for code in byteCharacters.utf8 {
                    binary.append(code)
                }
            }
            return binary
        }
        
        for code in data.utf8 {
            binary.append(code)
        }
        return binary
    }
    
    
    static func simpleDecoder(data: [UInt8], encoding: SimpleEncoding) -> String? {
        
        if let output = String(bytes: data, encoding: .ascii) {
            if encoding == .base64 {
                return output.base64Encoding()
            }
            return output
        }
        return nil
    }
}
