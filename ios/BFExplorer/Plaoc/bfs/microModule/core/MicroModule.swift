//
//  MicroModule.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/2/1.
//

import Foundation

typealias NativeOptions = [String:String]
typealias AppRun = (_ options: NativeOptions) -> Any
typealias Router = [String:AppRun]
typealias Mmid = String

protocol MicroModuleInfo {
    var mmid: Mmid { get }
}

/** 微组件抽象类 */
class MicroModule: MicroModuleInfo {
    var mmid: Mmid = ""
    
    internal func _bootstrap() async throws {}
    private var runningStateLock = false
    private var running: Bool {
        get {
            runningStateLock
        }
    }
    
    private func beforeBootstrap() async {
        if runningStateLock {
            fatalError("module \(self.mmid) already running")
        }
        
        runningStateLock = true
    }
    
    internal func afterBootstrap() async {}
    
    func bootstrap() async {
        await beforeBootstrap()
        
        do {
            try await self._bootstrap()
        } catch {
            
        }
        
        await self.afterBootstrap()
    }
    
    internal let _afterShutdownSignal = Signal<()>()
    
    internal func beforeShutdown() async {
        if !runningStateLock {
            fatalError("module \(mmid) already shutdown")
        }
        
        runningStateLock = false
    }
    
    internal func _shutdown() async throws {}
    
    internal func afterShutdown() async {
        await _afterShutdownSignal.emit(())
        _afterShutdownSignal.clear()
    }
    
    func shutdown() async {
        await beforeShutdown()
        
        do {
            try await _shutdown()
        } catch {
            
        }
        
        await afterShutdown()
    }
    
    internal func _connect(from: MicroModule) async -> Ipc {
        return Ipc()
    }
    
    func connect(from: MicroModule) async -> Ipc {
        if !runningStateLock {
            fatalError("module no running")
        }
        
        return await _connect(from: from)
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
