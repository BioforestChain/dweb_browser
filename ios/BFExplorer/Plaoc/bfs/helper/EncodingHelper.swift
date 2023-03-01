//
//  EncodingHelper.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/16.
//

import Foundation
import Vapor

enum SimpleEncoding: String {
    case base64 = "base64"
    case utf8 = "utf8"
}

func simpleEncoder(data: String, encoding: SimpleEncoding) -> [UInt8]? {
    if encoding == .base64 {
        guard let data = Data(base64Encoded: data) else { return nil }
        return Array(String(data: data, encoding: .utf8)!.utf8)
    }
    
    return Array(data.utf8)
}

func simpleDecoder(data: Data, encoding: SimpleEncoding) -> String? {
    if encoding == .base64 {
        return data.base32EncodedString()
    }
    
    return String(data: data, encoding: .utf8)
}

// TODO: 生成token待优化
func generateTokenBase64String(_ count: Int) -> String {
    var token = ""
    var bytes = [UInt8](repeating: 0, count: count)
    let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)

    if status == errSecSuccess {
        token = Data(bytes: bytes, count: count).base64EncodedString()
    }
    
    return token
}


//func dataUrlFromUtf8(utf8_string: String, asBase64: Bool, mime: String = "") -> Data {
//    let data_url = asBase64
//        ? "data:\(mime);base64,\(utf8_string.to_b64())"
//        : "data:\(mime);charset=UTF-8,\(utf8_string.encodeURIComponent())"
//
//    return data_url
//}
