//
//  MicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/24.
//

import UIKit
import RoutingKit
import Vapor

typealias AppRun = ([String:String]) -> Any
typealias Routers = [String:AppRun]

class MicroModule: MicroModuleInfo {
    
    var mmid: String = ""
    var routers: Routers?
    private var runningStateLock = PromiseOut<Bool>()
    private var running: Bool {
        return runningStateLock.value == true
    }
    
    internal var afterShutdownSignal = SimpleSignal()
    
    private var bootstrapContext: BootstrapContext?
    
    private var connectSignal = Signal<(Ipc,Request)>()
    
    var ipcSet = NSMutableSet()
    
    init() {
        runningStateLock.resolver(false)
        
    }
    
    func beforeBootstrap(bootstrapContext: BootstrapContext) {
        
        if runningStateLock.hasResult() {
            print("module \(self.mmid) already running")
            return
        }
        self.runningStateLock = PromiseOut<Bool>()
        self.bootstrapContext = bootstrapContext
    }
    
    private func afterBootstrap(dnsMM: BootstrapContext) {
        self.runningStateLock.resolver(true)
    }
    
    func bootstrap(bootstrapContext: BootstrapContext) {
        self.beforeBootstrap(bootstrapContext: bootstrapContext)
        
        defer {
            self.afterBootstrap(dnsMM: bootstrapContext)
        }
        
        do {
            try self._bootstrap(bootstrapContext: bootstrapContext)
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
        
        // 关闭所有的通讯
        
        ipcSet.enumerateObjects { ipc, _ in
            if let ipc = ipc as? Ipc {
                ipc.closeAction()
            }
        }
        ipcSet.removeAllObjects()
    }
    
    func _bootstrap(bootstrapContext: BootstrapContext) throws {  }
    
    func _shutdown() throws { }
    
    func afterShutdown() {
        
        afterShutdownSignal.emit(())
        afterShutdownSignal.clear()
        runningStateLock.resolver(false)
        self.bootstrapContext = nil
    }
    
    func shutdown() throws {
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
    
    func onConnect(cb: @escaping IpcConnect) -> OffListener {
        return connectSignal.listen(cb)
    }
    
    func connect(mmid: String, reason: Request? = nil) -> ConnectResult?  {
        return self.bootstrapContext?.dns.connect(mmid: mmid, reason: reason)
    }
    
    func beConnect(ipc: Ipc, reason: Request) {
        self.ipcSet.add(ipc)
        _ = ipc.onClose { _ in
            self.ipcSet.remove(ipc)
        }
        connectSignal.emit((ipc,reason))
    }
}

extension MicroModule {
    
    func nativeFetch(url: URL) -> Response? {
        
        return nativeFetch(urlstring: url.absoluteString)
    }
    
    func nativeFetch(urlstring: String) -> Response? {
        let request = Request.new(url: urlstring)
        return nativeFetch(request: request)
    }
    
    func nativeFetch(request: Request) -> Response? {
        for closure in nativeFetchAdaptersManager.adapters {
            if let response = closure(self, request) {
                return response
            }
        }
        return NativeFetch.localeFileFetch(remote: self, request: request) ?? NetworkManager.downLoadBodyByRequest(request: request)
    }
}

extension MicroModule {
    
    func startHttpDwebServer(options: DwebHttpServerOptions) -> ServerStartResult? {
        
        guard let url = URL(string: "file://http.sys.dweb/start?port=\(options.port)&subdomain=\(options.subdomain)") else { return nil }
        guard let response = self.nativeFetch(url: url) else { return nil }
        let model = ChangeTools.jsonToModel(jsonStr: response.body.string ?? "", ServerStartResult.self) as? ServerStartResult
        
        return model
    }
    
    func listenHttpDwebServer(startResult: ServerStartResult, routes: [RouteConfig]) -> ReadableStreamIpc {
        
        let ipc = ReadableStreamIpc(remote: self, role: "http-server/\(startResult.urlInfo.host)")
        
        guard let routing = ChangeTools.arrayValueString(routes) else { return ipc }
        
        let request = Request.new(url: "file://http.sys.dweb/listen?host=\(startResult.urlInfo.host)&token=\(startResult.token)&routes=\(routing))")
        let stream = self.nativeFetch(request: request)?.stream()
        
        ipc.bindIncomeStream(stream: stream, coroutineName: ipc.role ?? "")
        
        return ipc
    }
    
    func closeHttpDwebServer(options: DwebHttpServerOptions) -> Bool {
        
        guard let url = URL(string: "file://http.sys.dweb/start?port=\(options.port)&subdomain=\(options.subdomain)") else { return false }
        guard self.nativeFetch(url: url) != nil else { return false }
        return true
    }
    
    func createHttpDwebServer(options: DwebHttpServerOptions) -> HttpDwebServer? {
        guard let serverStartResult = startHttpDwebServer(options: options) else { return nil }
        return HttpDwebServer(nmm: self, options: options, startResult: serverStartResult)
    }
}
