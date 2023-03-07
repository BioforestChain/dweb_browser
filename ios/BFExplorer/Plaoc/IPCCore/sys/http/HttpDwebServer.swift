//
//  HttpDwebServer.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import Foundation

class HttpDwebServer: BaseModel {
    
    var nmm: MicroModule
    var options: DwebHttpServerOptions
    var startResult: ServerStartResult
    
    static var routeArray: [RouteConfig] {
        
        var list: [RouteConfig] = []
        list.append(RouteConfig(pathname: "", method: "GET"))
        list.append(RouteConfig(pathname: "", method: "POST"))
        list.append(RouteConfig(pathname: "", method: "PUT"))
        list.append(RouteConfig(pathname: "", method: "DELETE"))
        return list
    }
    
    init(nmm: MicroModule, options: DwebHttpServerOptions, startResult: ServerStartResult) {
        
        self.nmm = nmm
        self.options = options
        self.startResult = startResult
        
    }
    
    required init() {
        fatalError("init() has not been implemented")
    }
    
    func listen(routes: [RouteConfig] = routeArray) -> ReadableStreamIpc? {
        
        let po = PromiseOut<ReadableStreamIpc>()
        Task {
            if let streamIpc = self.nmm.listenHttpDwebServer(token: self.startResult.token, routes: routes) {
                po.resolver(streamIpc)
            }
        }
        let ipc = po.waitPromise()
        return ipc
    }
    
    lazy var close: Bool = {
        return self.nmm.closeHttpDwebServer(options: options)
    }()
}
