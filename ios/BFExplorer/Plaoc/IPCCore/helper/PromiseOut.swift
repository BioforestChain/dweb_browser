//
//  PromiseOut.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/15.
//

import UIKit
import PromiseKit

class PromiseOut<T> {
    
    private var resolver: Resolver<T>?
    private var rejecter: ((Error) -> Void)?
    
    public var promise: Promise<T>?
    
    private var tmpValue: T?
    
    var value: T? {
        return (promise?.isResolved ?? false) ? self.tmpValue : nil
    }
    
    init() {
        promise = Promise<T> { resolver in
            self.resolver = resolver
            self.rejecter = { error in
                resolver.reject(error)
            }
        }
    }
    
    func resolver(_ value: T) {
        resolver?.fulfill(value)
        self.tmpValue = value
    }
    
    func reject(_ error: Error) {
        rejecter?(error)
    }
    
    func waitPromise() -> T? {
        //放异步执行
        let result = try? self.promise?.wait()
        return result
    }
    
    func finished() -> Bool {
        return promise?.isResolved ?? false
    }
    
    func hasResult() -> Bool {
        return promise?.result != nil
    }
}

