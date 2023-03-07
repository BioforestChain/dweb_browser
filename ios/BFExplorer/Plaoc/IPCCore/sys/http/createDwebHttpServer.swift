//
//  createDwebHttpServer.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation

struct DwebHttpServerOptions {
    var port: Int
    var subdomain: String
    
    init(port: Int = 80, subdomain: String = "") {
        self.port = port
        self.subdomain = subdomain
    }
}
