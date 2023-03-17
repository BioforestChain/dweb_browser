//
//  PromiseOut.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/1.
//

import Foundation
import Combine
import Vapor

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

    func waitPromise() -> T {
        let sem = DispatchSemaphore(value: 0)

        Task {
            defer {
                sem.signal()
            }
            _ = await waitPromise()
        }

        sem.wait()
        return value!
    }

    func waitPromise() async -> T {
        return await withCheckedContinuation { continuation in
            self.cancellable = self._subject.sink(receiveCompletion: { complete in
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

    func promise() throws -> T {
        let sem = DispatchSemaphore(value: 0)
        Task {
            defer {
                sem.signal()
            }
            _ = try await promise()
        }

        sem.wait()
        return value!
    }

    func promise() async throws -> T {
        return try await withCheckedThrowingContinuation { continuation in
            self.cancellable = _subject.sink(receiveCompletion: { complete in
                switch complete {
                case .finished:
                    self.isFinished = true
                    continuation.resume(returning: self.value!)
                case .failure(let error):
                    continuation.resume(throwing: error)
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

class PromiseOut1<T> {
    var value: T?
    var e: Error?
    private let group = DispatchGroup()

    func resolve(_ value: T) {
        self.value = value
        group.leave()
        isResolved = true
        isFinished = true
    }

    func reject(_ e: Error) {
        self.e = e
        group.leave()
        isFinished = true
    }

    var isFinished: Bool = false
    var isResolved: Bool = false

    func waitPromise() -> T {
        group.enter()
        group.wait()

        if e != nil {
            fatalError(e!.localizedDescription)
        } else if value == nil {
            fatalError("value can not nil")
        }

        return value!
    }
}

class PromiseOut2<T> {
    private var promise: ((Result<T, String>) -> Void)?
    private var future: Future<T, String>?
    init() {
        self.future = Future<T, String>() { promise in
            self.promise = promise
        }
    }
    
    private let sem = DispatchSemaphore(value: 0)
    private var value: T?
    private var e: String?
    func resolve(_ value: T) {
        promise!(.success(value))
    }
    
    func reject(_ e: String) {
        promise!(.failure(e))
    }
    
    var isFinished = false
    var isResolved = false
    func waitPromise() throws -> T {
        let cancellable = future!.sink(receiveCompletion: { complete in
            switch complete {
            case .failure(let e):
                self.e = e
                self.sem.signal()
            case .finished:
                self.isFinished = true
            }
        }, receiveValue: { value in
            self.value = value
            self.isResolved = true
            self.sem.signal()
        })
        
        sem.wait()
        cancellable.cancel()
        if e != nil {
            throw e!
        } else if value == nil {
            throw "value is nil"
        }
        
        defer {
            isFinished = true
        }
        
        return value!
    }
}
extension String: LocalizedError {
    public var errorDescription: String? { return self }
}

class PromiseOut3<T> {
    var eventLoop: EventLoop
    lazy var promise: EventLoopPromise<T> = {
        self.eventLoop.makePromise(of: T.self)
    }()
    
    init(eventLoop: EventLoop = HttpServer.app.eventLoopGroup.next()) {
        self.eventLoop = eventLoop
    }
    
    func resolve(_ value: T) {
        promise.succeed(value)
        isResolved = true
    }
    
    func reject(_ e: String) {
        promise.fail(e)
    }
    
    var isResolved = false
    var isFinished = false
    
    func waitPromise() throws -> T {
        defer {
            isFinished = true
        }
        return try promise.futureResult.wait()
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
