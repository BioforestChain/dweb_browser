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
        self.role = role
        
        _ = port.onMessage { message in
//            var ipcMessage: IpcMessage
//            if let fromRequest = message as? IpcReqMessage {
//                ipcMessage = fromRequest
//            } else if let fromResponse = message as? IpcResMessage {
//                ipcMessage = fromResponse
//            } else {
//                ipcMessage = message
//            }
            
            self._messageSignal.emit((message, self))
            return nil
        }
        
        port.start()
    }
    
    override func _doPostMessage(data: IpcMessage) {
        port.postMessage(msg: data)
    }
    
    override func _doClose() async {
        port.close()
    }
}


class NativePort<I, O> {
    private let channel_in: PassthroughSubject<I, Never>
    private let channel_out: PassthroughSubject<O, Never>
    private let closePo: PromiseOut<()>
//    private var cancellable: AnyCancellable?
    
    init(channel_in: PassthroughSubject<I, Never>, channel_out: PassthroughSubject<O, Never>, closePo: PromiseOut<()>) {
        self.channel_in = channel_in
        self.channel_out = channel_out
        self.closePo = closePo
        
        Task {
            await closePo.waitPromise()
            closing = true
//            cancellable?.cancel()
            _closeSignal.emit(())
        }
    }
    
    private var started = false
    
    func start() {
        if started || closing {
            return
        } else {
            started = true
        }
        
        Task {
//            cancellable = channel_in.sink { message in
//                self._messageSignal.emit(message)
//            }
            for await message in channel_in.values {
                self._messageSignal.emit(message)
            }
        }
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
    
    func postMessage(msg: O) {
        channel_out.send(msg)
    }
    
    func onMessage(cb: @escaping (I) -> SIGNAL_CTOR?) -> () -> Bool {
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
