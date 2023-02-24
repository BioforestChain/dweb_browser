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
    
    func waitPromise() {
        //放异步执行
        DispatchQueue.global().async {
            let result = try? self.promise?.wait()
            print(result as Any)
        }
    }
    
    func finished() -> Bool {
        return promise?.isResolved ?? false
    }
}

