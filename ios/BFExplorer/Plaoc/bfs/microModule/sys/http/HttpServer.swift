//
//  HttpServer.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/13.
//

import Foundation
import Vapor

class HttpServer: ObservableObject {
    static let app = Application(.development, .createNew)
    static var address: String?
    static let PREFIX = "http://"
    static let PROTOCOL = "http:"
    static let PORT = 80
    static var origin = ""
    static var bindingPort = -1
    
    static func createServer(_ port: Int, _ host: String = "localhost") {
        address = "\(host):\(port)"
        configure(app, host: host, port: port)
        bindingPort = port
        origin = "\(PREFIX)localhost:\(port)"
    }
    
    static func configure(_ app: Application, host: String, port: Int) {
        app.http.server.configuration.hostname = host
        app.http.server.configuration.port = port
        app.http.server.configuration.supportPipelining = true
        app.http.server.configuration.responseCompression = .enabled(initialByteBufferCapacity: 1024)
        
        let cosConfiguration = CORSMiddleware.Configuration(
            allowedOrigin: .all, allowedMethods: [.POST, .GET, .PATCH, .PUT, .DELETE, .OPTIONS], allowedHeaders: [.userAgent]
        )
        let cosMiddleware = CORSMiddleware(configuration: cosConfiguration)
        app.middleware.use(cosMiddleware)

        app.routes.defaultMaxBodySize = "50MB"
    }
    
    static func shutdown() {
        app.server.shutdown()
    }
}

extension Application {
    
}
