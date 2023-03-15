//
//  StaticWebServer.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/8.
//

import UIKit

class StaticWebServer {

    var root: String = ""
    var entry: String = "index.html"
    var subdomain: String = ""
    var port: Int = 80
    
    init(root: String, entry: String, subdomain: String, port: Int) {
        self.root = root
        self.entry = entry
        self.subdomain = subdomain
        self.port = port
    }
}
