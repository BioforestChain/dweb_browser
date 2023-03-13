//
//  MicroModule.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/2/1.
//

import Foundation
import Vapor

typealias NativeOptions = [String:String]
typealias AppRun = (_ options: NativeOptions) -> Any
typealias Router = [String:AppRun]
typealias Mmid = String

protocol MicroModuleInfo {
    var mmid: Mmid { get }
}

/** 微组件抽象类 */
class MicroModule: MicroModuleInfo {
    typealias IpcConnectArgs = (Ipc, Request)
    
    var mmid: Mmid = ""
    
    private var runningStateLock = false
    private var running: Bool {
        get {
            runningStateLock
        }
    }
    
    private func beforeBootstrap(bootstrapContext: BootStrapContext) async {
        if runningStateLock {
            fatalError("module \(self.mmid) already running")
        }
        
//        runningStateLock = true
        _bootstrapContext = bootstrapContext
    }
    
    private var _bootstrapContext: BootStrapContext? = nil
    
    internal func afterBootstrap(dnsMM: BootStrapContext) async {
        runningStateLock = true
    }
    internal func _bootstrap(bootstrapContext: BootStrapContext) async throws {}
    func bootstrap(bootstrapContext: BootStrapContext) async {
        await beforeBootstrap(bootstrapContext: bootstrapContext)
        
        do {
            try await self._bootstrap(bootstrapContext: bootstrapContext)
        } catch {
            
        }
        
        await self.afterBootstrap(dnsMM: bootstrapContext)
    }
    
    internal let _afterShutdownSignal = Signal<()>()
    
    internal func beforeShutdown() async {
        if !runningStateLock {
            fatalError("module \(mmid) already shutdown")
        }
        
        runningStateLock = false
        
        /// 关闭所有的通讯
        for ipc in _ipcSet {
            await ipc.close()
        }
        
        _ipcSet.removeAll()
    }
    
    internal func _shutdown() async throws {}
    
    internal func afterShutdown() async {
        await _afterShutdownSignal.emit(())
        await _afterShutdownSignal.clear()
        runningStateLock = false
        _bootstrapContext = nil
    }
    
    func shutdown() async {
        await beforeShutdown()
        
        do {
            try await _shutdown()
        } catch {
            
        }
        
        await afterShutdown()
    }
    
    /**
     * 连接池
     */
    internal var _ipcSet: Set<Ipc> = []
    
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    private let _connectSignal = Signal<IpcConnectArgs>()
    
    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    internal func onConnect(cb: @escaping AsyncCallback<IpcConnectArgs, Any>) -> Signal.OffListener {
        _connectSignal.listen(cb)
    }
    
    /**
     * 尝试连接到指定对象
     */
    func connect(mmid: Mmid, reason: Request? = nil) async -> ConnectResult? {
        await _bootstrapContext?.dns.connect(mmid: mmid, reason: reason)
    }
    
    /**
     * 收到一个连接，触发相关事件
     */
    func beConnect(ipc: Ipc, reason: Request) async {
        _ipcSet.insert(ipc)
        
        _ = ipc.onClose {
            self._ipcSet.remove(ipc)
            return .OFF
        }
        
        await _connectSignal.emit((ipc, reason))
    }
}

// 用于字典存储key区分
extension MicroModule: Hashable {
    static func ==(lhs: MicroModule, rhs: MicroModule) -> Bool {
        return lhs.mmid == rhs.mmid
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(mmid)
    }
}
