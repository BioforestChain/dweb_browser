//
//  IpcMethod.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/20.
//

import Foundation
import Vapor

enum IpcMethod: String, Codable {
    case GET = "GET"
    case POST = "POST"
    case PUT = "PUT"
    case DELETE = "DELETE"
    case OPTIONS = "OPTIONS"
    case TRACE = "TRACE"
    case PATCH = "PATCH"
    case PURGE = "PURGE"
    case HEAD = "HEAD"
    
    static func from(vaporMethod: HTTPMethod) -> IpcMethod {
        return IpcMethod(rawValue: vaporMethod.rawValue)!
    }
}


