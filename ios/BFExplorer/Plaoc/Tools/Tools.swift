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
}
