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
}
