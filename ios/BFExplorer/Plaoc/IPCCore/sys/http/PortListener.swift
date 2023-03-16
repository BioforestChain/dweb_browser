//
//  PortListener.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/3.
//

import UIKit
import Vapor

class PortListener {

    var ipc: Ipc
    var host: String
    var routerSet = Set<StreamIpcRouter>()
    let destroySignal = SimpleSignal()
    
    init(ipc: Ipc, host: String) {
        
        self.ipc = ipc
        self.host = host
    }
    
    func addRouter(config: RouteConfig, streamIpc: ReadableStreamIpc) -> () -> Bool {
        let route = StreamIpcRouter(config: config, streamIpc: streamIpc)
        self.routerSet.insert(route)
        return { self.removeRouter(route: route) }
    }
    
    func removeRouter(route: StreamIpcRouter) -> Bool {
        return self.routerSet.remove(route) != nil
    }
    
    func hookHttpRequest(request: Request) -> Response? {
        
        for router in routerSet {
            let response = router.handler(request: request)
            return response
        }
        return nil
    }
    
    func onDestroy(cb: @escaping SimpleCallbcak) -> OffListener {
        return destroySignal.listen(cb)
    }
    
    func destroy() {
        routerSet.removeAll()
        destroySignal.emit(())
    }
}
