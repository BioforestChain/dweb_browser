//
//  createSignal.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import Foundation
import AVFoundation
import UIKit
import SwiftUI

typealias Callback = (_ firstPara: Any?, _ secondPara: Any?) -> Void


class Signal<T, R> {
    
    typealias SignalClosure = (T) -> R
    private var _cbs: Set<GenericsClosure<SignalClosure>> = []
    
    func listen(_ cb: @escaping SignalClosure) -> () -> Bool {
        let closureObj = GenericsClosure(closure: cb)
        self._cbs.insert(closureObj)
        
        var result: () -> Bool
        
        result = {
            let item = self._cbs.remove(closureObj)
            return item == nil ? false : true
        }
        
        return result
    }
    
    func emit(_ args: T) {
        for obj in self._cbs {
            obj.closure(args)
        }
    }
    
    static func createSignal() -> Signal<T, R> {
        return Signal<T, R>()
    }
}

// 通用的泛型闭包，用于Set存储
struct GenericsClosure<C>: Hashable {
    
    var timestamp: Int = Date().milliStamp
    var closure: C
    
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
