//
//  Data_extension.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/14.
//

import Foundation

extension Data {
    //Data转16进制
    func hexString() -> String {
        return map { String(format: "%02x", $0)}.joined(separator: "").uppercased()
    }
    
    /// Inputstream -> Data
    init(reading inputStream: InputStream) {
        self.init()
        inputStream.open()
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        while inputStream.hasBytesAvailable {
            let bytesRead = inputStream.read(buffer, maxLength: bufferSize)
            self.append(buffer, count: bytesRead)
        }
        inputStream.close()
    }
    
    func toInt() -> Int? {
        let stringInt = String(data: self, encoding: .utf8)
        return Int(stringInt ?? "")
    }
}
