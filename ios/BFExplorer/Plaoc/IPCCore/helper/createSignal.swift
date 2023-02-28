//
//  createSignal.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation
import UIKit



class Signal<T> {
    
    typealias SignalClosure = (T) -> Any
    
    private var _cbs: Set<GenericsClosure<SignalClosure>> = []
    
    private(set) var closure: GenericsClosure<SignalClosure>?
    
    func listen(_ cb: @escaping SignalClosure) -> GenericsClosure<SignalClosure> {
        let closureObj = GenericsClosure(closure: cb)
        self._cbs.insert(closureObj)
        
        self.closure = closureObj
        
        return closureObj
    }
    
    func removeCallback(cb: GenericsClosure<SignalClosure>) -> Bool {
        return self._cbs.remove(cb) != nil
    }
    
    func emit(_ args: T) {
        for obj in self._cbs {
            obj.closure!(args)
        }
    }
}

// 通用的泛型闭包，用于Set存储
struct GenericsClosure<C>: Hashable {
    
    var timestamp: Int = Date().milliStamp
    var closure: C?
    
    static func == (lhs: GenericsClosure, rhs: GenericsClosure) -> Bool {
        return lhs.timestamp == rhs.timestamp
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(timestamp)
    }
}


class SimpleSignal: Signal<()> {
    
    override func emit(_ args: Void) {
        super.emit(args)
    }
}
