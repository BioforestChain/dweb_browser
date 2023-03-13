//
//  NativeIpc.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import Combine

class NativeIpc: Ipc {
    let port: NativePort<IpcMessage, IpcMessage>
    
    init(port: NativePort<IpcMessage, IpcMessage>, remote: MicroModule, role: IPC_ROLE) {
        self.port = port
        super.init()
        self.remote = remote
        self.role = role.rawValue
        self.support_raw = true
        self.support_bianry = true
        
        _ = port.onMessage { message in
            await self._messageSignal.emit((message, self))
            return nil
        }
        
        port.start()
    }
    
    override func _doPostMessage(data: IpcMessage) async {
        await port.postMessage(msg: data)
    }
    
    override func _doClose() async {
        port.close()
    }
    
    override func toString() -> String {
        super.toString() + "@NativeIpc"
    }
}

class NativePortUid {
    static var uid_acc = 1
}
class NativePort<I, O> {
    private let channel_in: PassthroughSubject<I, Never>
    private let channel_out: PassthroughSubject<O, Never>
    private let closePo: PromiseOut<()>
    private var cancellable: AnyCancellable?
    
    private var uid = NativePortUid.uid_acc++
    
    func toString() -> String {
        "#p\(uid)"
    }
    
    init(channel_in: PassthroughSubject<I, Never>, channel_out: PassthroughSubject<O, Never>, closePo: PromiseOut<()>) {
        self.channel_in = channel_in
        self.channel_out = channel_out
        self.closePo = closePo
        
        Task {
            await closePo.waitPromise()
            closing = true
            cancellable?.cancel()
            await _closeSignal.emit(())
        }
    }
    
    private var started = false
    
    func start() {
        if started || closing {
            return
        } else {
            started = true
        }
        
        print("port-message-start/\(self)")
        cancellable = channel_in.sink(receiveValue: { message in
            Task {
                print("port-message-in/\(self) << \(message)")
                await self._messageSignal.emit(message)
                print("port-message-waiting/\(self)")
            }
        })
        print("port-message-end/\(self)")
    }
    
    private let _closeSignal = Signal<()>()
    
    private var closing = false
    
    func close() {
        if !closePo.isFinished {
            closePo.resolve(())
            print("port-closing/\(self)")
        }
    }
    
    private let _messageSignal = Signal<I>()
    
    func postMessage(msg: O) async {
        channel_out.send(msg)
        
        if let msg = msg as? IpcMessage {
            print(msg.type)
        }
    }
    
    func onMessage(cb: @escaping (I) async -> SIGNAL_CTOR?) -> () async -> Bool {
        self._messageSignal.listen(cb)
    }
}


struct NativeMessageChannel<T1, T2> {
    private let closePo = PromiseOut<()>()
    private let channel1 = PassthroughSubject<T1, Never>()
    private let channel2 = PassthroughSubject<T2, Never>()
    let port1: NativePort<T1, T2>
    let port2: NativePort<T2, T1>
    
    init() {
        port1 = NativePort(channel_in: channel1, channel_out: channel2, closePo: closePo)
        port2 = NativePort(channel_in: channel2, channel_out: channel1, closePo: closePo)
    }
}
