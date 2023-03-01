//
//  Http1Server.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/23.
//

import Foundation
import Vapor

class Http1Server {
    static let PREFIX = "http://"
    static let PROTOCOL = "http:"
    static let PORT = 80
    
    var bindingPort = -1
    
    private var server: HttpServer? = nil
    
    
}

//final class CustomServer: Server {
//    var onShutdown: NIOCore.EventLoopFuture<Void>
//
//    func shutdown() {
//
//    }
//}
//
//extension Application.Servers.Provider {
//    static var customServer: Self {
//        .init {
//            $0.servers.use { app in
//                CustomServer()
//            }
//        }
//    }
//}


