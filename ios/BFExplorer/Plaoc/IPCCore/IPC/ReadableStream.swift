//
//  ReadableStream.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/22.
//

import UIKit
import Combine
import Atomics

enum MyError: Error {
    case testError
}

typealias OnStartCallback = (ReadableStreamController) -> Void
typealias OnPullCallback = (Int, ReadableStreamController) -> Void

class ReadableStream: InputStream {
    
    var onStart: OnStartCallback?
    var onPull: OnPullCallback?
    
    private var data: Data = Data()
    private var ptr = 0  // 当前指针
    
    private let lock = NSLock()
    
    private var dataChannel = PassthroughSubject<Data, MyError>()
    private var closePo = PromiseOut<Void>()
    
    private var observerData = CurrentValueSubject<Int, Never>(0)
    
    private var _observerInt = 0 {
        didSet{
            observerData.send(_observerInt)
        }
    }
    
    static var id_acc = ManagedAtomic<Int>(1)
    
    var uid: String {
        return "#s\(ReadableStream.id_acc.store(1, ordering: .releasing))" + (cid != nil ? "(\(cid!))" : "")
    }
    
    var isClosed: Bool {
        return closePo.finished()
    }
    
    enum StreamControlSignal {
        case PULL
    }
    
    var cid: String?
    
    init(cid: String? = nil, onStart: @escaping OnStartCallback, onPull: @escaping OnPullCallback) {
        super.init(data: data)
        self.onStart = onStart
        self.onPull = onPull
        self.cid = cid
        
        onStart(controller)
        
        Task {
            for try await chunk in dataChannel.values {
                lock.withLock {
                    data += chunk
                }
                // 收到数据了，尝试解锁通知等待者
                _observerInt += 1
            }
            // 关闭数据通道了，尝试解锁通知等待者
            _observerInt = -1
            
            // 执行关闭
            self.closePo.resolver(())
        }
    }
    
    lazy private var controller: ReadableStreamController = {
        let controller = ReadableStreamController(dataChannel: dataChannel) {
            return self
        }
        return controller
    }()
    
    /**
         * 流协议不支持mark，读出来就直接丢了
         */
    func markSupported() -> Bool{
        return false
    }
    
    /** 执行垃圾回收
         * 10kb 的垃圾起，开始回收
         */
    func gc() {
        Task {
            lock.withLock {
                if ptr >= 10240 || isClosed {
                    let subData = data[ptr..<data.count]
                    data = subData
                    ptr = 0
                }
            }
        }
    }
    
    func afterClosed() {
        DispatchQueue.global().async {
            self.closePo.waitPromise()
        }
    }
    /**
         * 读取数据，在尽可能满足下标读取的情况下
         */
    private func requestData(requestSize: Int) -> [UInt8] {
        
        let ownSize = { self.data.count - self.ptr }
        if ownSize() >= requestSize {
            return [UInt8](data)
        }
        
        let wait = PromiseOut<Void>()
        let task = Task {
            if observerData.value == -1 {
                wait.resolver(())
            } else if ownSize() >= requestSize {
                wait.resolver(())
            } else {
                let desireSize = requestSize - data.count
                onPull?(desireSize, controller)
            }
        }
        
        wait.waitPromise()
        task.cancel()
        
        return [UInt8](data)
    }
    
    func toString() -> String {
        return self.uid
    }
    
    
}

extension ReadableStream {
    
    override func close() {
        
        if isClosed {
            return
        }
        
        closePo.resolver(())
        controller.close()
        super.close()
    }
    
    override func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) -> Int {
        
        var data = requestData(requestSize: len)
        if ptr >= data.count || len < 0 {
            return -1
        }
        
        if len == 0 {
            return 0
        }
        let length = super.read(buffer, maxLength: len)
        let readData = Data(bytes: buffer, count: length)
        
        data += readData
        ptr += length
        return length
    }
}


struct ReadableStreamController {
    
    private var dataChannel = PassthroughSubject<Data, MyError>()
    var stream: ReadableStream?
    
    init(dataChannel: PassthroughSubject<Data, MyError>, getStream: () -> ReadableStream) {
        self.dataChannel = dataChannel
        self.stream = getStream()
    }
    
    func enqueue(_ data: Data) {
        dataChannel.send(data)
    }
    
    func close() {
        dataChannel.send(completion: .finished)
    }
    
    func error(e: MyError) {
        dataChannel.send(completion: .failure(.testError))
    }
}
