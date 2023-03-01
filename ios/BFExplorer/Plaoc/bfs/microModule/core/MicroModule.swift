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

/** 微组件抽象类 */
class MicroModule {
    var mmid: Mmid = ""
    var routers: Router? {
        get {
            nil
        }
    }
    
    internal func _bootstrap() async throws {}
    private var running = false
    
    private func beforeBootstrap() async {
        if self.running {
            fatalError("module \(self.mmid) already running")
        }
        
        self.running = true
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
        if !running {
            fatalError("module \(mmid) already shutdown")
        }
        
        running = false
        _afterShutdownSignal.emit(())
    }
    
    internal func _shutdown() async throws {}
    
    internal func afterShutdown() async {}
    
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
        if !running {
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
