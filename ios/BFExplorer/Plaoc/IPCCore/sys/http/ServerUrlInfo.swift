//
//  ServerUrlInfo.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/3.
//

import UIKit
import Vapor

struct ServerUrlInfo {

    var host: String
    var internal_origin: String
    var public_origin: String
    
    init(host: String, internal_origin: String, public_origin: String) {
        self.host = host
        self.internal_origin = internal_origin
        self.public_origin = public_origin
    }
    //TODO
    func buildPublicUrl() -> URL? {
        return nil//URL(string: public_origin)!.appending("X-DWeb-Host", value: host)
    }
    
    func buildInternalUrl() -> URL? {
        return URL(string: internal_origin)
    }
}
