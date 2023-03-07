//
//  RouteConfig.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/3.
//

import UIKit

class RouteConfig: BaseModel {
    
    var pathname: String = ""
    var method: String = ""
    var matchMode: MatchMode = .PREFIX
    
    required init() {
        
    }
    
    init(pathname: String, method: String, matchMode: MatchMode = .PREFIX) {
        self.pathname = pathname
        self.method = method
        self.matchMode = matchMode
    }
}
