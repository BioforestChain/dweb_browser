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
    }
    
    func reject(_ error: Error) {
        rejecter?(error)
    }
}

