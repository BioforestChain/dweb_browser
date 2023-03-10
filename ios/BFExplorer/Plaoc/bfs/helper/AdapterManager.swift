//
//  AdapterManager.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/10.
//

import Foundation

class AdapterManager<T> {
    private var adapterOrderMap: [AdapterKey<T>:Int] = [:]
    private var orderAdapters: [T] = []
    
    struct AdapterKey<T>: Hashable {
        let timestamp = Date().milliStamp
        let adapter: T
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(timestamp)
        }
        
        static func ==(lhs: AdapterKey, rhs: AdapterKey) -> Bool {
            return lhs.timestamp == rhs.timestamp
        }
    }
    
    var adapters: [T] {
        get {
            orderAdapters
        }
    }
    
    func append(order: Int = 0, adapter: T) -> (()) -> Bool {
        let adapterKey = AdapterKey(adapter: adapter)
        adapterOrderMap[adapterKey] = order
        orderAdapters = adapterOrderMap.reduce(into: []) { $0.append(($1.key, $1.value)) }.sorted(by: { $0.1 < $1.1 }).map { $0.0.adapter }
        
        return { _ in
            self.remove(adapterKey: adapterKey)
        }
    }
    
    func remove(adapterKey: AdapterKey<T>) -> Bool {
        adapterOrderMap.removeValue(forKey: adapterKey) != nil
    }
}
