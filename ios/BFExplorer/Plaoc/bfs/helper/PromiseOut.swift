//
//  PromiseOut.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/1.
//

import Foundation
import Combine

class PromiseOut<T: Any> {
    static func resolve(_ value: T) -> PromiseOut<T> {
        let promiseOut = PromiseOut()
        promiseOut.resolve(value)
        return promiseOut
    }
    
    static func reject(_ e: Error) -> PromiseOut<T> {
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

//class PromiseOutTest {
//    func testResolve() {
//
//        Task.init {
//            let po = PromiseOut<()>()
//            let now = Date()
//
//            // 创建一个日期格式器
//            let dformatter = DateFormatter()
//            dformatter.dateFormat = "yyyy年MM月dd日 HH:mm:ss"
//            print("当前日期时间：\(dformatter.string(from: now))")
//
//            DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
//                po.resolve(())
//            }
//
//            print("start wait")
//            await po.waitPromise()
//            print("resolved")
//            print(Date().timeIntervalSince1970 - now.timeIntervalSince1970)
//        }
//    }
//
//    enum PromiseOutError: String, Error {
//        case reject = "reject"
//    }
//
//    func testReject() {
//        Task.init {
//            let po = PromiseOut<Unit>()
//
//            DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
//                po.reject(PromiseOutError.reject)
//            }
//
//            print("start wait")
//            await po.waitPromise()
//        }
//    }
//
//    func testMultiAwait() {
//        Task.init {
//            let po = PromiseOut<()>()
//            let now = Date()
//
//            Task {
//                try await Task.sleep(nanoseconds: 1_000_000_000)
//                po.resolve(())
//            }
//            Task {
//                try await Task.sleep(nanoseconds: 1_000_000_000)
//                po.resolve(())
//            }
//
//            Task {
//                print("start wait 1")
//                await po.waitPromise()
//                print("resolved 1")
//            }
//            Task {
//                print("start wait 2")
//                await po.waitPromise()
//                print("resolved 2")
//            }
//
//            print("start wait 3")
//            await po.waitPromise()
//            print("resolved 3")
//
//            print(Date().timeIntervalSince1970 - now.timeIntervalSince1970)
//        }
//    }
//
//    func bench() {
//        Task.init {
//
//        }
//    }
//}
//PromiseOutTest().testResolve()
//PromiseOutTest().testReject()
//PromiseOutTest().testMultiAwait()
