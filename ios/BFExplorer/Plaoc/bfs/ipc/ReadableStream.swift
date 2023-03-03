//
//  ReadableStreamIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/21.
//

import Foundation
import Combine
import Vapor

class ReadableStream: InputStream {
    private let dataChannel = PassthroughSubject<ByteBuffer, Never>()
    private lazy var controller = ReadableStreamController(dataChannel, getStream: { self })
    
    // 数据源
    private var _data: Data = Data()

    class ReadableStreamController {
        private let dataChannel: PassthroughSubject<ByteBuffer, Never>
        private let getStream: () -> ReadableStream
        var stream: ReadableStream {
            get {
                getStream()
            }
        }

        init(_ dataCannel: PassthroughSubject<ByteBuffer, Never>, getStream: @escaping () -> ReadableStream) {
            self.dataChannel = dataCannel
            self.getStream = getStream
        }

        func enqueue(_ data: ByteBuffer) {
            dataChannel.send(data)
        }
    }
    
    private let semaphore = DispatchSemaphore(value: -1)
    private var dataSize = 0
    private var dataSizeState: Int {
        get {
            self.dataSize
        }
        set {
            if newValue == -1 {
                self.semaphore.signal()
            }
            
            self.dataSize = newValue
        }
    }

    private var pullSignal = Signal<(Int)>()
    
    init(onStart: ((ReadableStreamController)) -> Void, onPull: @escaping ((Int, ReadableStreamController)) -> Void) {
        super.init(data: self._data)
        _ = onStart(controller)
        
        _ = pullSignal.listen { size in
            onPull((size, self.controller))
            return nil
        }
        
        Task {
            for await value in dataChannel.values {
                var buffer = value
                self._data.append(buffer.readData(length: buffer.readableBytes)!)
                self.dataSizeState = self._data.count
            }
            
            self.dataSizeState = -1
        }
    }
    
    private static var id_acc = 0
    private var uid: String {
        return "#s\(ReadableStream.id_acc++)"
    }
    func toString() -> String {
        return uid
    }
    
    override func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) -> Int {
        // 如果长度满足条件，直接返回
        if len <= _data.count {
            return super.read(buffer, maxLength: len)
        }
        
        dataSizeState = len - _data.count
        pullSignal.emit((dataSizeState))
        semaphore.wait()
        
        let len = _data.count
        _ = super.read(buffer, maxLength: len)
        
        return len
    }
}
