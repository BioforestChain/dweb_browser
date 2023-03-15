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
    
    func toBase64() -> String? {
        
        let base64String = self.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
        
        return base64String
    }
    
    func toUtf8() -> String? {
        return String(data: self, encoding: .utf8)
    }
    
    func toBase64Url() -> String? {
        return nil
    }
}
