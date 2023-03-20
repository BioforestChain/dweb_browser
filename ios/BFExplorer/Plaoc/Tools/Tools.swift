//
//  Tools.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/6.
//

import Foundation

class Tools {
    
    static func equals(_ x : Any, _ y : Any) -> Bool {
        guard x is AnyHashable else { return false }
        guard y is AnyHashable else { return false }
        return (x as! AnyHashable) == (y as! AnyHashable)
    }
    
    static func arc4randomByteArray(count: Int) -> String {
        var bytes = [UInt8]()
        for _ in 0..<count {
            let random = arc4random_uniform(255) - 128
            bytes.append(UInt8(random))
        }
        return bytes.toBase64Url()
    }
}
