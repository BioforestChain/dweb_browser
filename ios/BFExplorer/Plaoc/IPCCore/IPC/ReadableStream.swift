//
//  ReadableStream.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/22.
//

import UIKit
import Combine
import AsyncHTTPClient

enum MyError: Error {
    case testError
}

typealias OnStartCallback = (ReadableStreamController) -> Void
typealias OnPullCallback = (Int, ReadableStreamController) -> Void

class ReadableStream: InputStream {
    
    var onStart: OnStartCallback?
    var onPull: OnPullCallback?
    
    private var data: [UInt8] = [UInt8]()
    private var ptr = 0
    private var mark = 0
    
    private var dataChannel = PassthroughSubject<[UInt8], MyError>()
    private var dataSizeChangeChannel = PassthroughSubject<Int, MyError>()
    private var pullSignal = Signal<Int>()
    
    private var controller: ReadableStreamController?
    private var writeDataScope = DispatchQueue.init(label: "write")
    private var readDataScope = DispatchQueue.init(label: "read")
    private var closePo = PromiseOut<Int>()
    private var isClose: Bool = true
    
    private var id_acc = 0
    
    private var uid: String {
        let tmp = self.id_acc
        self.id_acc += 1
        return "#$\(tmp)"
    }
    
    enum StreamControlSignal {
        case PULL
    }
    
    var test: String = ""
    
    func startLoad(onStart: @escaping OnStartCallback, onPull: @escaping OnPullCallback) -> ReadableStream {
        
        let stream = ReadableStream()
        stream.open()
        stream.controller = ReadableStreamController(dataChannel: dataChannel, stream: self)
        onStart(stream.controller!)
        
//        self.onStart = onStart
//        self.onPull = onPull
        
        stream.isClose = stream.closePo.finished()
        
      _ = pullSignal.listen { desiredSize in
            onPull(desiredSize,stream.controller!)
        }
        
        writeDataScope.async {
            _ = self.dataChannel.sink { complete in
                self.dataSizeChangeChannel.send(-1)
                self.closePo.resolver(0)
            } receiveValue: { value in
                self.data += value
                self.dataSizeChangeChannel.send(self.data.count)
            }
            
        }
        return stream
    }
    
    func afterClosed() {
        closePo.waitPromise()
    }
    
    private func requestData(ptr: Int) -> [UInt8] {
        if ptr < data.count {
            return [UInt8](data)
        }
        
        let desiredSize = ptr + 1
        
        let semaphore = DispatchSemaphore(value: 0)
        writeAction(desiredSize: desiredSize, semaphore: semaphore)
        readAction(semaphore: semaphore)
        
        semaphore.wait()
        semaphore.wait()
        return [UInt8](data)
    }
    
    func writeAction(desiredSize: Int, semaphore: DispatchSemaphore) {
        writeDataScope.async {
            self.pullSignal.emit(desiredSize)
            semaphore.signal()
        }
    }
    
    func readAction(semaphore: DispatchSemaphore) {
        readDataScope.async {
            let wait = PromiseOut<UInt>()
            self.dataSizeChangeChannel.sink { complete in
                print("complete")
            } receiveValue: { value in
                wait.resolver(UInt(value))
            }
            wait.waitPromise()
            semaphore.signal()
        }
    }
    
    func toString() -> String {
        return self.uid
    }
    
    
}

extension ReadableStream {
    
    func read() -> Int {
        let data = requestData(ptr: ptr)
        if ptr < data.count {
            let count = ptr
            ptr += 1
            return Int(data[count])
        }
        return -1
    }
    
    func available() -> Int {
        return requestData(ptr: ptr).count - ptr
    }
    
    func markAction(limit: Int) {
        mark = limit
    }
    
    func reset() {
        if mark < 0 || mark >= data.count {
            print("标识不对")
            return
        }
        ptr = mark
    }
    
    override func close() {
        super.close()
        controller?.close()
        ptr = data.count
    }
    
    override func read(_ buffer: UnsafeMutablePointer<UInt8>, maxLength len: Int) -> Int {
        
        var data = requestData(ptr: data.count + len - 1)
        if ptr >= data.count || len < 0 {
            return -1
        }
        
        if len == 0 {
            return 0
        }
        
        let length = (available() < len) ? available() : len
        data += [buffer.pointee]
        ptr += length
        return length
    }
}


struct ReadableStreamController {
    
    private var dataChannel = PassthroughSubject<[UInt8], MyError>()
    var stream: ReadableStream?
    
    init(dataChannel: PassthroughSubject<[UInt8], MyError>, stream: ReadableStream) {
        self.dataChannel = dataChannel
        self.stream = stream
    }
    
    func enqueue(byteArray: [UInt8]) {
        dataChannel.send(byteArray)
    }
    
    func close() {
        dataChannel.send(completion: .finished)
    }
    
    func error(e: MyError) {
        dataChannel.send(completion: .failure(.testError))
    }
}
