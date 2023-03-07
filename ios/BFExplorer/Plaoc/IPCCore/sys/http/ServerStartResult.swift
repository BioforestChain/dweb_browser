//
//  ServerStartResult.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/4.
//

import Foundation

class ServerStartResult: BaseModel {
    
    var token: String
    var urlInfo: ServerUrlInfo
    
    init(token: String, urlInfo: ServerUrlInfo) {
        self.token = token
        self.urlInfo = urlInfo
    }
    
    required init() {
        fatalError("init() has not been implemented")
    }
}
