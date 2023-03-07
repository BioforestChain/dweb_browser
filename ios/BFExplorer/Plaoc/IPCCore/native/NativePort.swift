//
//  NativePort.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Combine

class NativePort<I, O> {

    typealias Callbcak = (I) -> Any
    
    private let channel_in: PassthroughSubject<I, Never>
    private let channel_out: PassthroughSubject<O, Never>
    private var cancellable: AnyCancellable?
    private let closePo: PromiseOut<Void>
    
    private var uid_acc: Int = 1
    private var started: Bool = false
    
    var messageSignal = Signal<I>()
    private var closeSignal = SimpleSignal()
    
    private var uid: Int {
        let tmp = uid_acc
        uid_acc += 1
        return tmp
    }
    
    init(channel_in: PassthroughSubject<I, Never>, channel_out: PassthroughSubject<O, Never>, closePo: PromiseOut<Void>) {
        self.channel_in = channel_in
        self.channel_out = channel_out
        self.closePo = closePo
        
        Task {
            _ = closePo.waitPromise()
            channel_out.send(completion: .finished)
            closeSignal.emit(())
        }
    }
    
    func toString() -> String {
        return "#p\(uid)"
    }
    
    func start() {
        if started || closePo.finished() {
            return
        } else {
            self.started = true
        }
        
        cancellable = channel_in.sink(receiveValue: { message in
            self.messageSignal.emit(message)
        })
    }
    
    func onClose(cb: @escaping SimpleCallbcak) -> OffListener {
        return closeSignal.listen(cb)
    }
    
    func close() {
        if !closePo.finished() {
            closePo.resolver(())
            
        }
    }
    
    //发送消息
    func postMessage(msg: O) {
        channel_out.send(msg)
    }
    
    //监听消息
    func onMessage(cb: @escaping Callbcak) -> OffListener {
        return messageSignal.listen(cb)
    }
}
