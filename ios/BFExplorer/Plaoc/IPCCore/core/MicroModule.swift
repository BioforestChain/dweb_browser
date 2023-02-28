//
//  MicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/24.
//

import UIKit
import RoutingKit

typealias AppRun = ([String:String]) -> Any

class MicroModule: NSObject {
    
    var mmid: String = ""
    var routers: [String:AppRun]?
    private var runningStateLock = PromiseOut<Bool>()
    private var running: Bool {
        return runningStateLock.value == true
    }
    
    private var afterShutdownSignal = SimpleSignal()
    
    override init() {
        super.init()
        runningStateLock.resolver(false)
        
    }
    
    func beforeBootstrap() {
        
        if runningStateLock.hasResult() {
            print("module \(self.mmid) already running")
            return
        }
        self.runningStateLock = PromiseOut<Bool>()
        
    }
    
    private func afterBootstrap() {
        self.runningStateLock.resolver(true)
    }
    
    func bootstrap() {
        self.beforeBootstrap()
        
        defer {
            self.afterBootstrap()
        }
        
        do {
            try self._bootstrap()
        } catch {
            print(error.localizedDescription)
        }
    }
    
    private func beforeShutdown() {
        if !runningStateLock.hasResult() {
            print("module \(self.mmid) already shutdown")
            return
        }
        self.runningStateLock = PromiseOut<Bool>()
    }
    
    func _bootstrap() throws {  }
    
    func _shutdown() throws { }
    
    func afterShutdown() {
        runningStateLock.resolver(false)
    }
    
    func shutdown() {
        self.beforeShutdown()
        
        defer {
            self.afterShutdown()
        }
        
        do {
            try self._shutdown()
        } catch {
            print(error.localizedDescription)
        }
    }
    
    func _connect(from: MicroModule) -> Ipc? { return nil }
    
    func connect(from: MicroModule) -> Ipc? {
        
        if !runningStateLock.hasResult() {
            print("module no running")
            return nil
        }
        return _connect(from: from)
    }
}

