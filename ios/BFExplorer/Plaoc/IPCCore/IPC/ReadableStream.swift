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
    private var ptr = 0  // 当前指针
    private var mark = 0  //标记
    
    private var dataChannel = PassthroughSubject<[UInt8], MyError>()
    private var dataSizeChangeChannel = PassthroughSubject<Int, MyError>()
    private var writelable: AnyCancellable?
    private var readlable: AnyCancellable?
    private var pullSignal = Signal<Int>()
    
    private var writeDataScope = DispatchQueue.init(label: "write")
    private var readDataScope = DispatchQueue.init(label: "read")
    private var closePo = PromiseOut<Void>()
    private var isClose: Bool = true
    
    private var id_acc = 1
    
    private var uid: String {
        let tmp = self.id_acc
        self.id_acc += 1
        return "#s\(tmp)"
    }
    
    enum StreamControlSignal {
        case PULL
    }
    
    var test: String = ""
    
    lazy private var controller: ReadableStreamController = {
        let controller = ReadableStreamController(dataChannel: dataChannel) {
            return self
        }
        return controller
    }()
    
    func startLoad(onStart: @escaping OnStartCallback, onPull: @escaping OnPullCallback) -> ReadableStream {
        
        let stream = ReadableStream()
        stream.open()
        stream.controller = controller
        onStart(stream.controller)
        
        self.onStart = onStart
        self.onPull = onPull
        
        stream.isClose = stream.closePo.finished()
//
//      _ = pullSignal.listen { desiredSize in
//            onPull(desiredSize,stream.controller)
//        }
        
        writeDataScope.async {
            self.writelable = self.dataChannel.sink { complete in
                self.dataSizeChangeChannel.send(-1)
                self.closePo.resolver(())
            } receiveValue: { value in
                self.data += value
                self.dataSizeChangeChannel.send(self.data.count)
            }
            
        }
        return stream
    }
    
    func afterClosed() {
        DispatchQueue.global().async {
            self.closePo.waitPromise()
        }
    }
    
    private func requestData(ptr: Int) -> [UInt8] {
        if ptr < data.count {
            return [UInt8](data)
        }
        
        let endSize = ptr + 1
        let desiredSize = endSize - self.ptr
        
        let semaphore = DispatchSemaphore(value: 0)
        writeAction(desiredSize: desiredSize, semaphore: semaphore)
        readAction(ptr: ptr, semaphore: semaphore)
        
        semaphore.wait()
        semaphore.wait()
        return [UInt8](data)
    }
    
    func writeAction(desiredSize: Int, semaphore: DispatchSemaphore) {
        writeDataScope.async {
//            self.pullSignal.emit(desiredSize)
            self.onPull?(desiredSize,self.controller)
            semaphore.signal()
        }
    }
    
    func readAction(ptr: Int, semaphore: DispatchSemaphore) {
        readDataScope.async {
            let wait = PromiseOut<Void>()
            self.readlable = self.dataSizeChangeChannel.sink { complete in
                print("complete")
            } receiveValue: { value in
                if value == self.data.count {
                    print("REQUEST-DATA/WAITING/\(self.uid)")
                } else if value == -1 {
                    wait.resolver(())
                } else if ptr < value {
                    wait.resolver(())
                }
            }
            _ = wait.waitPromise()
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
        controller.close()
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
    
    init(dataChannel: PassthroughSubject<[UInt8], MyError>, getStream: () -> ReadableStream) {
        self.dataChannel = dataChannel
        self.stream = getStream()
    }
    
    func enqueue(byteArray: [UInt8]) {
        guard byteArray.count > 0 else { return }
        dataChannel.send(byteArray)
    }
    
    func close() {
        dataChannel.send(completion: .finished)
    }
    
    func error(e: MyError) {
        dataChannel.send(completion: .failure(.testError))
    }
}
