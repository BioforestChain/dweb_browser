//
//  Int_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/14.
//

import Foundation

extension Int {
    
    mutating func toByteArray() -> Data {
        
        let packet =  Data(bytes: &self, count: MemoryLayout.size(ofValue: self))
        return packet
    }
}
