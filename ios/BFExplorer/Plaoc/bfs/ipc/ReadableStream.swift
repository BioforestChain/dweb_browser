//
//  ReadableStreamIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/21.
//

import Foundation
import Combine
import Vapor
import Atomics

class ReadableStream: InputStream {
    var cid: String? = nil
    var onPull: AsyncCallback<(Int, ReadableStreamController), Void>
    
    // 数据源
    private var _data: Data = Data()
    
    class ReadableStreamController {
        let dataChannel: PassthroughSubject<Data, Never>
        let getStream: () -> ReadableStream
        
        init(dataChannel: PassthroughSubject<Data, Never>, getStream: @escaping () -> ReadableStream) {
            self.dataChannel = dataChannel
            self.getStream = getStream
        }
        
        var stream: ReadableStream {
            get {
                getStream()
            }
        }
        
        func enqueue(_ data: Data) {
            dataChannel.send(data)
        }
        
        func close() {}
        
        func error() {}
    }
    
    private var dataChannel = PassthroughSubject<Data, Never>()
    private lazy var controller: ReadableStreamController = {
        ReadableStreamController(dataChannel: dataChannel, getStream: { self })
    }()
    
    private var _dataLock = NSLock()
    
    init(
        cid: String? = nil,
        onStart: @escaping Callback<ReadableStreamController, Void>,
        onPull: @escaping AsyncCallback<(Int, ReadableStreamController), Void>
    ) {
        self.cid = cid
        self.onPull = onPull
        self.uid = "#s\(ReadableStream.id_acc.store(1, ordering: .releasing))" + (cid != nil ? "(\(cid!))" : "")
        super.init(data: _data)
        
        onStart(controller)
        Task {
//            await onStart(controller)
            for await chunk in dataChannel.values {
                _dataLock.withLock {
                    _data += chunk
                    print("DATA-IN/\(self.uid)", "+\(chunk.count) ~> \(_data.count)")
                }
                // 收到数据了，尝试解锁通知等待者
                _observerInt += 1
            }
            
            // 关闭数据通道了，尝试解锁通知等待者
            _observerInt = -1
            
            // 执行关闭
            closePo.resolve(())
        }
    }
    
    private var _observerInt = 0 {
        didSet{
            observerData.send(_observerInt)
        }
    }
    private var observerData = CurrentValueSubject<Int, Never>(0)
    
    private let closePo = PromiseOut<()>()
    
    func afterClosed() async {
        await closePo.waitPromise()
    }
    
    var isClosed: Bool {
        get {
            closePo.isFinished
        }
    }
    
    static var id_acc = ManagedAtomic<Int>(1)
    
    var uid: String
    
    func toString() -> String {
        uid
    }
    
//    func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) async -> Int {
//        await withCheckedContinuation { continuation in
//            // 如果下标满足条件，直接返回
//            if _data.count >= len {
//                continuation.resume(returning: super.read(buffer, maxLength: len))
//            }
//
//            let semaphore = DispatchSemaphore(value: 0)
//            let task = Task {
//                if observerData.value == -1 {
//                    print("REQUEST-DATA/END/\(uid)", "\(_data.count)/\(len)")
//                    semaphore.signal()
//                } else if _data.count >= len {
//                    print("REQUEST-DATA/CHANGER/\(uid)", "\(_data.count)/\(len)")
//                    semaphore.signal()
//                } else {
//                    print("REQUEST-DATA/WAITING-&-PULL/\(uid)", "\(_data.count)/\(len)")
//                    let desireSize = len - _data.count
//                    await onPull((desireSize, controller))
//                }
//            }
//
//            semaphore.wait()
//            task.cancel()
//
//            if len < 0 {
//                continuation.resume(returning: -1)
//            } else if len == 0 {
//                continuation.resume(returning: 0)
//            } else {
//                continuation.resume(returning: super.read(buffer, maxLength: _data.count))
//            }
//        }
//    }
    
    private func requestData(requestSize: Int) -> Data {
        // 如果下标满足条件，直接返回
        if _data.count >= requestSize {
            return _data
        }
        
//        let wait = PromiseOut<()>()
        let sem = DispatchSemaphore(value: 0)
        let task = Task {
            if observerData.value == -1 {
                print("REQUEST-DATA/END/\(uid)", "\(_data.count)/\(requestSize)")
//                wait.resolve(())
                sem.signal()
            } else if _data.count >= requestSize {
                print("REQUEST-DATA/CHANGER/\(uid)", "\(_data.count)/\(requestSize)")
//                wait.resolve(())
                sem.signal()
            } else {
                print("REQUEST-DATA/WAITING-&-PULL/\(uid)", "\(_data.count)/\(requestSize)")
                let desireSize = requestSize - _data.count
                await onPull((desireSize, controller))
            }
        }
        
        sem.wait()
//        try! wait.waitPromise()
//        try? wait.promise()
        task.cancel()
        
        return _data
    }
    
    /**
     * 可读数据长度
     */
//    func available() async -> Int {
//        requestData(requestSize: 1).count
//    }
    func available() -> Int {
        requestData(requestSize: 1).count
    }
    
    override func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) -> Int {
        let data = requestData(requestSize: len)
        var len = data.count
        
        if len < 0 {
            // 流已读完
            return -1
        } else if len == 0 {
            return 0
        }
        
        // 处理最后一次读取的时候可能不没有len的长度，取实际长度
        len = _data.count < len ? _data.count : len
        
        // 返回读取的长度
        return super.read(buffer, maxLength: len)
    }
    
    override func close() {
        if isClosed {
            return
        }
        print("CLOSE/\(uid)")
        
        closePo.resolve(())
        controller.close()
        // 关闭的时候不会马上清空数据，还是能读出来最后的数据的
        
        super.close()
    }
}

