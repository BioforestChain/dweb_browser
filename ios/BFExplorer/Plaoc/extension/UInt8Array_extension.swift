//
//  UInt8Arrat_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/14.
//

import Foundation

extension [UInt8] {
    
    func toInt() -> Int {
        let source = self.withUnsafeBytes { $0.load(as: UInt32.self) }
        let value = CFByteOrderGetCurrent() == CFByteOrder(CFByteOrderLittleEndian.rawValue) ? UInt32(bigEndian: source) : source
        return Int(value)
    }
    
    // base64编码
    func toBase64() -> String {
        let plainData = Data(bytes: self, count: self.count)
        
        let base64String = plainData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
        
        return base64String ?? ""
        
    }
    // utf8
    func toUtf8() -> String {
        let plainData = Data(bytes: self, count: self.count)
        return String(data: plainData, encoding: .utf8) ?? ""
    }
    
    func toBase64Url() -> String {
        let content = toBase64()
        let encodeUrlString = content.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
        return encodeUrlString ?? ""
    }
}


