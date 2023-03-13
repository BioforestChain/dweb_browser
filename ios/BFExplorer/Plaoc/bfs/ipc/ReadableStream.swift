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
        onStart: @escaping AsyncCallback<ReadableStreamController, Void>,
        onPull: @escaping AsyncCallback<(Int, ReadableStreamController), Void>
    ) {
        self.cid = cid
        self.onPull = onPull
        self.uid = "#s\(ReadableStream.id_acc.store(1, ordering: .releasing))" + (cid != nil ? "(\(cid!))" : "")
        super.init(data: _data)
        
        Task {
            await onStart(controller)
            
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
    
    func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) async -> Int {
        await withCheckedContinuation { continuation in
            // 如果下标满足条件，直接返回
            if _data.count >= len {
                continuation.resume(returning: super.read(buffer, maxLength: len))
            }
            
            let semaphore = DispatchSemaphore(value: 0)
            let task = Task {
                if observerData.value == -1 {
                    print("REQUEST-DATA/END/\(uid)", "\(_data.count)/\(len)")
                    semaphore.signal()
                } else if _data.count >= len {
                    print("REQUEST-DATA/CHANGER/\(uid)", "\(_data.count)/\(len)")
                    semaphore.signal()
                } else {
                    print("REQUEST-DATA/WAITING-&-PULL/\(uid)", "\(_data.count)/\(len)")
                    let desireSize = len - _data.count
                    await onPull((desireSize, controller))
                }
            }
            
            semaphore.wait()
            task.cancel()
            
            if len < 0 {
                continuation.resume(returning: -1)
            } else if len == 0 {
                continuation.resume(returning: 0)
            } else {
                continuation.resume(returning: super.read(buffer, maxLength: _data.count))
            }
        }
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


//class ReadableStream: InputStream {
//    private let dataChannel = PassthroughSubject<ByteBuffer, Never>()
//    private lazy var controller = ReadableStreamController(dataChannel, getStream: { self })
//
//    // 数据源
//    private var _data: Data = Data()
//
//    class ReadableStreamController {
//        private let dataChannel: PassthroughSubject<ByteBuffer, Never>
//        private let getStream: () -> ReadableStream
//        var stream: ReadableStream {
//            get {
//                getStream()
//            }
//        }
//
//        init(_ dataCannel: PassthroughSubject<ByteBuffer, Never>, getStream: @escaping () -> ReadableStream) {
//            self.dataChannel = dataCannel
//            self.getStream = getStream
//        }
//
//        func enqueue(_ data: ByteBuffer) {
//            dataChannel.send(data)
//        }
//    }
//
//    private let semaphore = DispatchSemaphore(value: -1)
//    private var dataSize = 0
//    private var dataSizeState: Int {
//        get {
//            self.dataSize
//        }
//        set {
//            if newValue == -1 {
//                self.semaphore.signal()
//            }
//
//            self.dataSize = newValue
//        }
//    }
//
//    private var pullSignal = Signal<(Int)>()
//
//    init(onStart: ((ReadableStreamController)) -> Void, onPull: @escaping ((Int, ReadableStreamController)) async -> Void) {
//        super.init(data: self._data)
//        _ = onStart(controller)
//
//        _ = pullSignal.listen { size in
//            await onPull((size, self.controller))
//            return nil
//        }
//
//        Task {
//            for await value in dataChannel.values {
//                var buffer = value
//                self._data.append(buffer.readData(length: buffer.readableBytes)!)
//                self.dataSizeState = self._data.count
//            }
//
//            self.dataSizeState = -1
//        }
//    }
//
//    private static var id_acc = 0
//    private var uid: String {
//        return "#s\(ReadableStream.id_acc++)"
//    }
//    func toString() -> String {
//        return uid
//    }
//
//    override func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) -> Int {
//        // 如果长度满足条件，直接返回
//        if len <= _data.count {
//            return super.read(buffer, maxLength: len)
//        }
//
//        dataSizeState = len - _data.count
//        Task {
//            await pullSignal.emit((dataSizeState))
//        }
//        semaphore.wait()
//
//        let len = _data.count
//        _ = super.read(buffer, maxLength: len)
//
//        return len
//    }
//}
