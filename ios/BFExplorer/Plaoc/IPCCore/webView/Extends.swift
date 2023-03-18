//
//  Extends.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/17.
//

import UIKit

class Extends<T: Any>: NSObject {

    private var instanceMap = NSMutableDictionary()
    private var methodCache: [String: [T]] = [:]
    
    func  add(instance: T, config: Config = Config()) -> Bool {
        
        if let con = instanceMap[instance] as? Config, con == config {
            return false
        }
        
        instanceMap[instance] = config
        resetCacheBy(instance: instance)
        return true
    }
    
    func remove(instance: T) -> Bool {
        
        if instanceMap.object(forKey: instance) != nil {
            instanceMap.removeObject(forKey: instance)
            resetCacheBy(instance: instance)
            return true
        }
        return false
    }
    
    private func resetCacheBy(instance: T) {
        
        let obj = toNSObject(value: instance)
        let methodList = obj.totalMethod()
        for method in methodList {
            methodCache.removeValue(forKey: method)
        }
    }
    
    func hasMethod(methodName: String) -> [T] {
        
        var list = methodCache[methodName]
        if list == nil {
            list = instanceMap.filter { self.isOverriding(instance: $0.0 as! T, methodName: methodName)}
                .sorted(by: {($0.1 as! Config).order < ($1.1 as! Config).order})
                .map { $0.0 as! T }
            methodCache[methodName] = list
        }
        return list!
    }
    
    private func isOverriding(instance: T, methodName: String) -> Bool {
        
        let obj = toNSObject(value: instance)
        let methodList = obj.totalMethod()
        return methodList.contains(methodName)
    }
    
    private func toNSObject<T> (value: T) -> NSObject {
        return _NSObject(_Object<T>(value))
    }
}

class Config: NSObject {
    
    var order: Int = 0
    init(order: Int = 0) {
        self.order = order
    }
    
    
}

class _NSObject : NSObject {
  var object: AnyObject
  init (_ object: AnyObject) { self.object = object }
}

/// An Any disguised as an AnyObject
class _Object <T> {
  var value: T
  init (_ value: T) { self.value = value }
}
