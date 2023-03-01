//
//  PortListener.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/23.
//

import Foundation
import Vapor

enum MatchMode: String, Codable {
    case FULL = "full"
    case PREFIX = "prefix"
}

struct RouteConfig {
    var pathname: String
    var method: IpcMethod
    var matchMode: MatchMode = .PREFIX
}

class StreamIpcRouter {
    let config: RouteConfig
    let streamIpc: ReadableStreamIpc
    var isMatch: (_ request: Request) -> Bool
    
    init(config: RouteConfig, streamIpc: ReadableStreamIpc) {
        self.config = config
        self.streamIpc = streamIpc
        
        switch config.matchMode {
        case .PREFIX:
            isMatch = { request in
                config.method == IpcMethod.from(vaporMethod: request.method) && request.url.path.hasPrefix(config.pathname)
            }
        case .FULL:
            isMatch = { request in
                config.method == IpcMethod.from(vaporMethod: request.method) && request.url.path == config.pathname
            }
        }
    }
    
    func handler(request: Request) async -> Response? {
        if isMatch(request) {
            return await streamIpc.request(request: request)
        } else {
            return nil
        }
    }
}

extension StreamIpcRouter: Hashable {
    func hash(into hasher: inout Hasher) {
        hasher.combine(streamIpc)
    }
    
    static func ==(lhs: StreamIpcRouter, rhs: StreamIpcRouter) -> Bool {
        return lhs.streamIpc == rhs.streamIpc
    }
}

class PortListener {
    let ipc: Ipc
    let host: String
    
    init(ipc: Ipc, host: String) {
        self.ipc = ipc
        self.host = host
    }
    
    private var _routerSet: Set<StreamIpcRouter> = []
    
    func addRouter(config: RouteConfig, streamIpc: ReadableStreamIpc) -> () -> Bool {
        let route = StreamIpcRouter(config: config, streamIpc: streamIpc)
        _routerSet.insert(route)
        
        return {
            self._routerSet.remove(route)
            return true
        }
    }
    
    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    func hookHttpRequest(request: Request) async -> Response? {
        for router in _routerSet {
            let response = await router.handler(request: request)
            
            if response != nil {
                return response
            }
        }
        
        return nil
    }
    
    /// 销毁
    private let destroySignal = Signal<()>()
    func onDestroy(cb: @escaping () -> SIGNAL_CTOR?) -> () -> Bool {
        destroySignal.listen(cb)
    }
    
    func destroy() async {
        _routerSet.removeAll()
        destroySignal.emit(())
    }
}
