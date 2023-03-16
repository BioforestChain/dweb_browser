//
//  AdapterManager.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import Foundation

class AdapterManager<T> {
    
    private var adapterOrderMap = NSMutableDictionary()
    private var orderdAdapters: [T] = []
    
    var adapters: [T] {
        return orderdAdapters
    }
    
    func append(order: Int = 0, adapter: T) -> () -> Bool {
        
        adapterOrderMap[adapter] = order
        orderdAdapters = adapterOrderMap.sorted(by: {($0.1 as! Int) < ($1.1 as! Int)}).map { $0.0 as! T }
        return { self.remove(adapter: adapter) }
    }
    
    func remove(adapter: T) -> Bool {

        if adapterOrderMap.object(forKey: adapter) != nil {
            adapterOrderMap.removeObject(forKey: adapter)
            return true
        }
        return false
    }
}
