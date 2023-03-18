//
//  NSObject_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/17.
//

import Foundation

extension NSObject {
    
    func totalProperty() -> [String] {
        
        var count: UInt32 = 0
        let cls: AnyClass? = object_getClass(self)
        
        let plist = class_copyPropertyList(cls, &count)
        var propertys: [String] = []
        for item in 0..<count {
            if let property = plist?[Int(item)] {
                let cname = property_getName(property)
                let name = String(cString: cname)
                propertys.append(name)
            }
        }
        return propertys
    }
    
    func totalMethod() -> [String] {
        var count: UInt32 = 0
        let cls: AnyClass? = object_getClass(self)
        
        let methodList = class_copyMethodList(cls, &count)
        var methods: [String] = []
        for item in 0..<count {
            if let method = methodList?[Int(item)] {
                let cname = method_getName(method)
                let name = NSStringFromSelector(cname)
                methods.append(name)
            }
        }
        return methods
    }
}
