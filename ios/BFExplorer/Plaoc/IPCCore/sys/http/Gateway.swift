//
//  Gateway.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation

class Gateway {
    
    var listener: PortListener
    var urlInfo: ServerUrlInfo
    var token: String
    
    init(listener: PortListener, urlInfo: ServerUrlInfo, token: String) {
        self.listener = listener
        self.urlInfo = urlInfo
        self.token = token
    }
}
