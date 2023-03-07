//
//  HTTPServer.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor

class HTTPServer: ObservableObject {
    
    static let shared = HTTPServer()
    let app = Application(.development)
    private let PREFIX = "http://"
    private let PROTOCOL = "http:"
    static let PORT = 80
    
    private var bindingPort = -1
    
    var authority: String {
        return "localhost:\(bindingPort)"
    }
    
    var origin: String {
        return "\(PREFIX)\(authority)"
    }
    
    func createServer(_ port: Int, _ host: String = "localhost") {
        
        configure(app, host: host, port: port)
    }
    
    private func configure(_ app: Application, host: String, port: Int) {
        app.http.server.configuration.hostname = host
        app.http.server.configuration.port = port
        app.http.server.configuration.supportPipelining = true
        app.routes.defaultMaxBodySize = "50MB"
    }
    
    func start() {
        try? app.server.start()
    }
    
    func shutdown() {
        app.server.shutdown()
    }
}

