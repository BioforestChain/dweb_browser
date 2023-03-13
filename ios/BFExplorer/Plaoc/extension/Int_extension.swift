//
//  Int_extension.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/13.
//

import Foundation

extension Int {
    mutating func toData() -> Data {
        Data(bytes: &self, count: MemoryLayout.size(ofValue: self))
    }
}
