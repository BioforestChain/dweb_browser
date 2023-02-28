//
//  NativeMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit

class NativeMicroModule: MicroModule {

    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    typealias NativaCallback = (NativeIpc) -> Any
    
    private var connectedIpcSet = Set<Ipc>()
    
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    private let connectSignal = Signal<NativeIpc>()
    
    //TODO
//    private var  apiRouting: RoutingHttpHandler?
    
    override init() {
        super.init()
        onConnect(cb: { clientIpc in
            clientIpc.onRequest { request,ipc in
                
            }
        })
    }
    
    override func _connect(from: MicroModule) -> NativeIpc? {
        let channel = NativeMessageChannel<IpcMessage, IpcMessage>()
        let nativeIpc = NativeIpc(port: channel.port1, remote: from, role: .SERVER)
        
        self.connectedIpcSet.insert(nativeIpc)
        _ = nativeIpc.onClose { _ in
            self.connectedIpcSet.remove(nativeIpc)
        }
        
        self.connectSignal.emit(nativeIpc)
        return NativeIpc(port: channel.port2, remote: self, role: .CLIENT)
    }
    
    func onConnect(cb: @escaping NativaCallback) -> GenericsClosure<NativaCallback> {
        return self.connectSignal.listen(cb)
    }
    //在模块关停后，从自身构建的通讯通道都要关闭掉
    override func afterShutdown() {
        super.afterShutdown()
        for ipc in self.connectedIpcSet {
            ipc.closeAction()
        }
        self.connectedIpcSet.removeAll()
    }
    
    private func defineHandler(handler: (URLRequest) -> Any?) {
        
    }
    
    private func defineHandler(handler: (URLRequest, Ipc) -> Any?) {
        defineHandler { request, ipc in
            handler(request, ipc)
        }
    }
}
