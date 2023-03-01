//
//  PromiseOut.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/1.
//

import Foundation
import Combine

class PromiseOut<T: Any> {
    static func resolve(value: T) -> PromiseOut<T> {
        let promiseOut = PromiseOut()
        promiseOut.resolve(value)
        return promiseOut
    }
    
    static func reject(e: Error) -> PromiseOut<T> {
        let promiseOut = PromiseOut()
        promiseOut.reject(e)
        return promiseOut
    }
    
    private let _subject = PassthroughSubject<T, Error>()
    
    func resolve(_ value: T) {
        _subject.send(value)
    }
    
    func reject(_ e: Error) {
        _subject.send(completion: .failure(e))
    }
    
    var value: T?
    private var cancellable: AnyCancellable?
    var isFinished: Bool = false
    var isResolved: Bool = false
    
    func waitPromise() async -> T {
        return await withCheckedContinuation { continuation in
            self.cancellable = _subject.sink(receiveCompletion: { complete in
                switch complete {
                case .finished:
                    self.isFinished = true
                    continuation.resume(returning: self.value!)
                case .failure(let error):
                    fatalError("PromiseOut reject \(error.localizedDescription)")
                }
            }, receiveValue: { value in
                self.value = value
                self.isResolved = true
                self._subject.send(completion: .finished)
            })
        }
    }
    
    deinit {
        cancellable?.cancel()
    }
}
