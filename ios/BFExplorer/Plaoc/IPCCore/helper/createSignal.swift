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
/*
 class Signal<Element> {
 
 var items = Set<GenericsClosure<Element>>()
 
 private var type: String = ""
 
 
 init(type: String) {
 self.type = type
 
 }
 
 func listen(callback: GenericsClosure<Element>) -> () -> Bool {
 
 items.insert(callback)
 
 var result: () -> Bool
 
 result = {
 let item = self.items.remove(callback)
 return item == nil ? false : true
 }
 return result
 
 }
 
 func emit(firstPara: Any?, secondPara: Any?) {
 for cb in items {
 cb.callback?(firstPara, secondPara)
 }
 }
 
 static func createSignal(type: String) -> Signal {
 return Signal(type: type)
 }
 }
 *//*
    struct GenericsClosure<T>: Hashable {
    
    
    var type: String?
    var callback: Callback?
    
    init(type: String? = nil, callback: Callback? = nil) {
    self.type = type
    self.callback = callback
    }
    
    static func == (lhs: GenericsClosure<T>, rhs: GenericsClosure<T>) -> Bool {
    return lhs.type == rhs.type
    }
    
    func hash(into hasher: inout Hasher) {
    hasher.combine(type)
    }
    }
    */
